package org.proyecto.business;

import org.proyecto.data.AdjuntoRepository;
import org.proyecto.data.CategoriaRepository;
import org.proyecto.data.HistorialAsignacionRepository;
import org.proyecto.data.OperadorRepository;
import org.proyecto.data.PrioridadRepository;
import org.proyecto.data.TicketEstadoRepository;
import org.proyecto.data.TicketRepository;
import org.proyecto.entities.Adjunto;
import org.proyecto.entities.Categoria;
import org.proyecto.entities.HistorialAsignacion;
import org.proyecto.entities.HistorialEstado;
import org.proyecto.entities.Operador;
import org.proyecto.entities.Prioridad;
import org.proyecto.entities.Ticket;
import org.proyecto.entities.TicketEstado;
import org.proyecto.entities.TicketEstadoCodigo;
import org.proyecto.entities.TipoAccionAsignacion;
import org.proyecto.entities.UsuarioTicket;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Máquina de estados del ticket según los diagramas de transición del documento (Recepción de
 * reclamos + Priorización/validación + Asignación + Cierre de tickets):
 *
 * NUEVO -> [clasificar CU04 / asignar prioridad CU05] -> [validar CU06] -> VALIDADO
 *   -> PENDIENTE_CORRECCION -> (usuario corrige, CU06 FA2) -> NUEVO
 * VALIDADO -> [asignar operador CU13] -> EN_ANALISIS
 * EN_ANALISIS -> [resolver] -> RESUELTO -> [cerrar CU17] -> CERRADO
 * CERRADO -> [validar resolución CU18, conforme] -> CERRADO (queda cerrado)
 * CERRADO -> [reabrir CU19] -> REABIERTO -> [resolver de nuevo] -> RESUELTO -> ...
 */
@Service
public class TicketService {

    private static final Set<String> EXTENSIONES_PERMITIDAS =
            Set.of("jpg", "jpeg", "png", "pdf", "docx", "xlsx");

    private static final long TAMANIO_MAXIMO_BYTES = 5L * 1024 * 1024;

    private static final Set<TicketEstadoCodigo> ESTADOS_EDITABLES_ADJUNTOS =
            Set.of(TicketEstadoCodigo.NUEVO, TicketEstadoCodigo.PENDIENTE_CORRECCION, TicketEstadoCodigo.EN_ANALISIS);

    private final TicketRepository ticketRepository;
    private final AdjuntoRepository adjuntoRepository;
    private final HistorialAsignacionRepository historialAsignacionRepository;
    private final TicketEstadoRepository ticketEstadoRepository;
    private final CategoriaRepository categoriaRepository;
    private final PrioridadRepository prioridadRepository;
    private final OperadorRepository operadorRepository;
    private final NotificacionService notificacionService;

    public TicketService(TicketRepository ticketRepository, AdjuntoRepository adjuntoRepository,
                          HistorialAsignacionRepository historialAsignacionRepository,
                          TicketEstadoRepository ticketEstadoRepository, CategoriaRepository categoriaRepository,
                          PrioridadRepository prioridadRepository, OperadorRepository operadorRepository,
                          NotificacionService notificacionService) {
        this.ticketRepository = ticketRepository;
        this.adjuntoRepository = adjuntoRepository;
        this.historialAsignacionRepository = historialAsignacionRepository;
        this.ticketEstadoRepository = ticketEstadoRepository;
        this.categoriaRepository = categoriaRepository;
        this.prioridadRepository = prioridadRepository;
        this.operadorRepository = operadorRepository;
        this.notificacionService = notificacionService;
    }

    // ---- CU01 - Registrar ticket ----
    public Ticket registrarTicket(UsuarioTicket usuario, String titulo, String descripcion,
                                   Long idCategoria, Long idPrioridadSugerida) {
        if (titulo == null || titulo.isBlank()) {
            throw new InvalidTicketOperationException("El título es obligatorio");
        }
        if (descripcion == null || descripcion.isBlank()) {
            throw new InvalidTicketOperationException("La descripción es obligatoria");
        }

        Ticket ticket = new Ticket();
        ticket.setTitulo(titulo);
        ticket.setDescripcion(descripcion);
        ticket.setUsuario(usuario);
        ticket.setFechaCreacion(LocalDateTime.now());
        if (idCategoria != null) {
            ticket.setCategoria(buscarCategoria(idCategoria));
        }
        if (idPrioridadSugerida != null) {
            ticket.setPrioridadSugerida(buscarPrioridad(idPrioridadSugerida));
        }

        aplicarEstado(ticket, TicketEstadoCodigo.NUEVO, usuario, null, "Registro del reclamo", null);
        return ticketRepository.save(ticket);
    }

    // ---- CU02 - Adjuntar archivo ----
    public Adjunto adjuntarArchivo(Long ticketId, Long usuarioId, String nombreArchivo,
                                    String tipoArchivo, long tamano, byte[] contenido, String descripcion) {
        Ticket ticket = buscarTicketPropio(ticketId, usuarioId);

        if (!ESTADOS_EDITABLES_ADJUNTOS.contains(ticket.getEstadoActual().getCodigo())) {
            throw new InvalidTicketOperationException(
                    "El ticket no admite nuevos adjuntos en su estado actual (" + ticket.getEstadoActual().getNombre() + ")");
        }
        if (!tieneExtensionPermitida(nombreArchivo)) {
            throw new InvalidTicketOperationException(
                    "Formato de archivo no permitido. Formatos válidos: JPG, PNG, PDF, DOCX, XLSX");
        }
        if (tamano > TAMANIO_MAXIMO_BYTES) {
            throw new InvalidTicketOperationException("El archivo supera el tamaño máximo permitido (5 MB)");
        }

        Adjunto adjunto = new Adjunto();
        adjunto.setTicket(ticket);
        adjunto.setNombreArchivo(nombreArchivo);
        adjunto.setTipoArchivo(tipoArchivo);
        adjunto.setTamano(tamano);
        adjunto.setDescripcion(descripcion);
        adjunto.setFechaCarga(LocalDateTime.now());
        adjunto.setContenido(contenido);
        return adjuntoRepository.save(adjunto);
    }

    // ---- CU03 - Consultar mis tickets ----
    public List<Ticket> consultarTicketsDeUsuario(Long usuarioId) {
        return ticketRepository.findByUsuarioIdOrderByFechaCreacionDesc(usuarioId);
    }

    public Ticket obtenerDetalle(Long ticketId) {
        return buscarTicketExistente(ticketId);
    }

    public List<Adjunto> listarAdjuntos(Long ticketId) {
        return adjuntoRepository.findByTicketIdOrderByFechaCargaAsc(ticketId);
    }

    public List<HistorialAsignacion> listarHistorialAsignaciones(Long ticketId) {
        return historialAsignacionRepository.findByTicketIdOrderByFechaAsignacionAsc(ticketId);
    }

    // ---- CU04 - Clasificar ticket ----
    public Ticket clasificar(Long ticketId, Long idCategoria) {
        Ticket ticket = buscarTicketExistente(ticketId);
        requireEstadoClasificable(ticket);

        Categoria categoria = buscarCategoria(idCategoria);
        ticket.setCategoria(categoria);
        ticket.setFechaUltimaActualizacion(LocalDateTime.now());
        return ticketRepository.save(ticket);
    }

    // ---- CU05 - Asignar prioridad ----
    public Ticket asignarPrioridad(Long ticketId, Long idPrioridad) {
        Ticket ticket = buscarTicketExistente(ticketId);
        requireEstadoClasificable(ticket);
        if (ticket.getCategoria() == null) {
            throw new InvalidTicketOperationException("El ticket debe estar clasificado antes de asignar prioridad");
        }

        Prioridad prioridad = buscarPrioridad(idPrioridad);
        ticket.setPrioridad(prioridad);
        ticket.setFechaUltimaActualizacion(LocalDateTime.now());
        return ticketRepository.save(ticket);
    }

    // ---- CU06 - Validar ticket ----
    public Ticket validarTicket(Long ticketId, Operador operador) {
        Ticket ticket = buscarTicketExistente(ticketId);
        requireEstadoClasificable(ticket);
        if (ticket.getCategoria() == null || ticket.getPrioridad() == null) {
            throw new InvalidTicketOperationException(
                    "El ticket debe estar clasificado y priorizado antes de validarse");
        }

        aplicarEstado(ticket, TicketEstadoCodigo.VALIDADO, null, operador, "Validado por operador", null);
        return ticketRepository.save(ticket);
    }

    public Ticket marcarIncompleto(Long ticketId, Operador operador, String observacion) {
        if (observacion == null || observacion.isBlank()) {
            throw new InvalidTicketOperationException("Debe indicar qué información falta o es inconsistente");
        }
        Ticket ticket = buscarTicketExistente(ticketId);
        requireEstadoClasificable(ticket);

        aplicarEstado(ticket, TicketEstadoCodigo.PENDIENTE_CORRECCION, null, operador, observacion, null);
        ticket = ticketRepository.save(ticket);
        notificacionService.notificarUsuario(ticket.getUsuario(),
                "Tu ticket #" + ticket.getId() + " necesita corrección: " + observacion);
        return ticket;
    }

    /** FA2 de CU06: el usuario corrige y reenvía; el ticket vuelve a NUEVO para revalidación. */
    public Ticket reenviarTicket(Long ticketId, Long usuarioId, String titulo, String descripcion) {
        Ticket ticket = buscarTicketPropio(ticketId, usuarioId);
        if (ticket.getEstadoActual().getCodigo() != TicketEstadoCodigo.PENDIENTE_CORRECCION) {
            throw new InvalidTicketOperationException("El ticket no está pendiente de corrección");
        }
        if (titulo != null && !titulo.isBlank()) {
            ticket.setTitulo(titulo);
        }
        if (descripcion != null && !descripcion.isBlank()) {
            ticket.setDescripcion(descripcion);
        }
        aplicarEstado(ticket, TicketEstadoCodigo.NUEVO, ticket.getUsuario(), null,
                "Usuario corrigió la información", null);
        return ticketRepository.save(ticket);
    }

    public List<Ticket> listarPendientesClasificacion() {
        return ticketRepository.findByEstadoActual_CodigoInOrderByFechaCreacionAsc(
                List.of(TicketEstadoCodigo.NUEVO, TicketEstadoCodigo.PENDIENTE_CORRECCION));
    }

    // ---- CU13 - Asignar ticket ----
    public Ticket asignarOperador(Long ticketId, Long idOperador, Operador responsable) {
        Ticket ticket = buscarTicketExistente(ticketId);
        if (ticket.getEstadoActual().getCodigo() != TicketEstadoCodigo.VALIDADO) {
            throw new InvalidTicketOperationException("El ticket debe estar VALIDADO para poder asignarse");
        }
        if (ticket.getOperador() != null) {
            throw new InvalidTicketOperationException("El ticket ya tiene un operador asignado, usá reasignar");
        }

        Operador nuevoOperador = buscarOperadorActivo(idOperador);
        ticket.setOperador(nuevoOperador);
        aplicarEstado(ticket, TicketEstadoCodigo.EN_ANALISIS, null, responsable, "Ticket asignado", null);
        ticket = ticketRepository.save(ticket);

        registrarAsignacion(ticket, null, nuevoOperador, responsable, "Asignación inicial", TipoAccionAsignacion.ASIGNACION);
        notificacionService.notificarOperador(nuevoOperador,
                "Se te asignó el ticket #" + ticket.getId() + ": " + ticket.getTitulo());
        return ticket;
    }

    // ---- CU14 - Reasignar ticket ----
    public Ticket reasignarOperador(Long ticketId, Long idOperadorNuevo, String motivo, Operador responsable) {
        if (motivo == null || motivo.isBlank()) {
            throw new InvalidTicketOperationException("El motivo de la reasignación es obligatorio");
        }
        Ticket ticket = buscarTicketExistente(ticketId);
        if (ticket.getOperador() == null) {
            throw new InvalidTicketOperationException("El ticket no tiene operador asignado, usá asignar");
        }
        if (ticket.getEstadoActual().getCodigo() == TicketEstadoCodigo.CERRADO) {
            throw new InvalidTicketOperationException("No se puede reasignar un ticket cerrado");
        }

        Operador operadorAnterior = ticket.getOperador();
        Operador operadorNuevo = buscarOperadorActivo(idOperadorNuevo);
        if (operadorAnterior.getId().equals(operadorNuevo.getId())) {
            throw new InvalidTicketOperationException("Debe seleccionarse un operador distinto al actual");
        }

        ticket.setOperador(operadorNuevo);
        ticket.setFechaUltimaActualizacion(LocalDateTime.now());
        ticket = ticketRepository.save(ticket);

        registrarAsignacion(ticket, operadorAnterior, operadorNuevo, responsable, motivo, TipoAccionAsignacion.REASIGNACION);
        notificacionService.notificarOperador(operadorNuevo,
                "Se te reasignó el ticket #" + ticket.getId() + ": " + motivo);
        notificacionService.notificarOperador(operadorAnterior,
                "El ticket #" + ticket.getId() + " fue reasignado: " + motivo);
        return ticket;
    }

    public List<Ticket> listarAsignadosA(Long operadorId) {
        return ticketRepository.findByOperadorIdOrderByFechaCreacionDesc(operadorId);
    }

    public List<Ticket> listarSinAsignar() {
        return ticketRepository.findByEstadoActual_CodigoAndOperadorIsNullOrderByFechaCreacionAsc(
                TicketEstadoCodigo.VALIDADO);
    }

    // ---- Transición operativa hacia RESUELTO (precondición de CU17) ----
    public Ticket marcarResuelto(Long ticketId, Operador operador) {
        Ticket ticket = buscarTicketExistente(ticketId);

        if (ticket.getOperador() == null || !ticket.getOperador().getId().equals(operador.getId())) {
            throw new InvalidTicketOperationException("Solo el operador asignado puede resolver el ticket");
        }
        TicketEstadoCodigo estadoActual = ticket.getEstadoActual().getCodigo();
        if (estadoActual != TicketEstadoCodigo.EN_ANALISIS && estadoActual != TicketEstadoCodigo.REABIERTO) {
            throw new InvalidTicketOperationException(
                    "El ticket no se puede marcar como resuelto desde su estado actual (" + ticket.getEstadoActual().getNombre() + ")");
        }

        aplicarEstado(ticket, TicketEstadoCodigo.RESUELTO, null, operador, "Marcado como resuelto", null);
        return ticketRepository.save(ticket);
    }

    // ---- CU17 - Cerrar ticket ----
    public Ticket cerrarTicket(Long ticketId, Operador operador, String motivoCierre) {
        Ticket ticket = buscarTicketExistente(ticketId);

        if (ticket.getOperador() == null || !ticket.getOperador().getId().equals(operador.getId())) {
            throw new InvalidTicketOperationException("Solo el operador asignado puede cerrar el ticket");
        }
        if (ticket.getEstadoActual().getCodigo() != TicketEstadoCodigo.RESUELTO) {
            throw new InvalidTicketOperationException("El ticket debe estar RESUELTO para poder cerrarlo");
        }
        if (motivoCierre == null || motivoCierre.isBlank()) {
            throw new InvalidTicketOperationException("La descripción de la solución es obligatoria");
        }

        ticket.setMotivoCierre(motivoCierre);
        ticket.setFechaCierre(LocalDateTime.now());
        aplicarEstado(ticket, TicketEstadoCodigo.CERRADO, null, operador, "Cierre del ticket", motivoCierre);
        ticket = ticketRepository.save(ticket);
        notificacionService.notificarUsuario(ticket.getUsuario(),
                "Tu ticket #" + ticket.getId() + " fue cerrado. Solución: " + motivoCierre);
        return ticket;
    }

    // ---- CU18 - Validar resolución ----
    public Ticket validarResolucion(Long ticketId, UsuarioTicket usuario) {
        Ticket ticket = buscarTicketPropio(ticketId, usuario.getId());

        if (ticket.getEstadoActual().getCodigo() != TicketEstadoCodigo.CERRADO) {
            throw new InvalidTicketOperationException("El ticket debe estar CERRADO para poder validarlo");
        }

        ticket.setValidado(true);
        ticket.setFechaUltimaActualizacion(LocalDateTime.now());
        ticket = ticketRepository.save(ticket);
        if (ticket.getOperador() != null) {
            notificacionService.notificarOperador(ticket.getOperador(),
                    "El usuario validó la resolución del ticket #" + ticket.getId());
        }
        return ticket;
    }

    // ---- CU19 - Reabrir ticket ----
    public Ticket reabrirTicketPorUsuario(Long ticketId, UsuarioTicket usuario, String motivo) {
        Ticket ticket = buscarTicketPropio(ticketId, usuario.getId());
        return reabrir(ticket, usuario, null, motivo);
    }

    public Ticket reabrirTicketPorOperador(Long ticketId, Operador operador, String motivo) {
        Ticket ticket = buscarTicketExistente(ticketId);
        if (ticket.getOperador() == null || !ticket.getOperador().getId().equals(operador.getId())) {
            throw new TicketNotFoundException(ticketId);
        }
        return reabrir(ticket, null, operador, motivo);
    }

    private Ticket reabrir(Ticket ticket, UsuarioTicket actorUsuario, Operador actorOperador, String motivo) {
        if (ticket.getEstadoActual().getCodigo() != TicketEstadoCodigo.CERRADO) {
            throw new InvalidTicketOperationException("El ticket debe estar CERRADO para poder reabrirlo");
        }
        if (!ticket.getEstadoActual().isPermiteReapertura()) {
            throw new InvalidTicketOperationException("El estado actual no permite reapertura");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new InvalidTicketOperationException("El motivo de la reapertura es obligatorio");
        }

        ticket.setMotivoReapertura(motivo);
        ticket.setValidado(false);
        aplicarEstado(ticket, TicketEstadoCodigo.REABIERTO, actorUsuario, actorOperador, motivo, null);
        ticket = ticketRepository.save(ticket);

        if (ticket.getOperador() != null) {
            notificacionService.notificarOperador(ticket.getOperador(),
                    "El ticket #" + ticket.getId() + " fue reabierto: " + motivo);
        }
        if (actorOperador != null) {
            notificacionService.notificarUsuario(ticket.getUsuario(),
                    "El ticket #" + ticket.getId() + " fue reabierto por el operador: " + motivo);
        }
        return ticket;
    }

    // ---- Helpers ----

    private HistorialEstado aplicarEstado(Ticket ticket, TicketEstadoCodigo codigo, UsuarioTicket actorUsuario,
                                           Operador actorOperador, String observacion, String motivoCambio) {
        TicketEstado estado = ticketEstadoRepository.findByCodigo(codigo)
                .orElseThrow(() -> new IllegalStateException("Estado no seedeado: " + codigo));
        ticket.setEstadoActual(estado);
        ticket.setFechaUltimaActualizacion(LocalDateTime.now());

        HistorialEstado historial = new HistorialEstado();
        historial.setTicket(ticket);
        historial.setTicketEstado(estado);
        historial.setFechaCambio(LocalDateTime.now());
        historial.setObservacion(observacion);
        historial.setMotivoCambio(motivoCambio);
        historial.setActorUsuario(actorUsuario);
        historial.setActorOperador(actorOperador);
        ticket.getHistorialEstados().add(historial);
        return historial;
    }

    private void registrarAsignacion(Ticket ticket, Operador anterior, Operador nuevo, Operador responsable,
                                      String motivo, TipoAccionAsignacion tipoAccion) {
        HistorialAsignacion historial = new HistorialAsignacion();
        historial.setTicket(ticket);
        historial.setOperadorAnterior(anterior);
        historial.setOperadorNuevo(nuevo);
        historial.setOperadorResponsable(responsable);
        historial.setMotivo(motivo);
        historial.setTipoAccion(tipoAccion);
        historial.setFechaAsignacion(LocalDateTime.now());
        historialAsignacionRepository.save(historial);
    }

    private void requireEstadoClasificable(Ticket ticket) {
        TicketEstadoCodigo codigo = ticket.getEstadoActual().getCodigo();
        if (codigo != TicketEstadoCodigo.NUEVO && codigo != TicketEstadoCodigo.PENDIENTE_CORRECCION) {
            throw new InvalidTicketOperationException(
                    "El ticket no admite esta operación en su estado actual (" + ticket.getEstadoActual().getNombre() + ")");
        }
    }

    private Ticket buscarTicketExistente(Long ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));
    }

    private Ticket buscarTicketPropio(Long ticketId, Long usuarioSolicitanteId) {
        Ticket ticket = buscarTicketExistente(ticketId);
        if (!ticket.getUsuario().getId().equals(usuarioSolicitanteId)) {
            throw new TicketNotFoundException(ticketId);
        }
        return ticket;
    }

    private Categoria buscarCategoria(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new InvalidTicketOperationException("Categoría inexistente: " + id));
    }

    private Prioridad buscarPrioridad(Long id) {
        return prioridadRepository.findById(id)
                .orElseThrow(() -> new InvalidTicketOperationException("Prioridad inexistente: " + id));
    }

    private Operador buscarOperadorActivo(Long id) {
        Operador operador = operadorRepository.findById(id)
                .orElseThrow(() -> new InvalidTicketOperationException("Operador inexistente: " + id));
        if (!operador.isActivo()) {
            throw new InvalidTicketOperationException("El operador seleccionado no está activo");
        }
        return operador;
    }

    private boolean tieneExtensionPermitida(String nombreArchivo) {
        if (nombreArchivo == null) {
            return false;
        }
        int puntoIndex = nombreArchivo.lastIndexOf('.');
        if (puntoIndex < 0 || puntoIndex == nombreArchivo.length() - 1) {
            return false;
        }
        String extension = nombreArchivo.substring(puntoIndex + 1).toLowerCase(Locale.ROOT);
        return EXTENSIONES_PERMITIDAS.contains(extension);
    }
}

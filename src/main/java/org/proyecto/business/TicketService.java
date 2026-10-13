package org.proyecto.business;

import org.proyecto.data.AdjuntoRepository;
import org.proyecto.data.CategoriaRepository;
import org.proyecto.data.ComentarioRepository;
import org.proyecto.data.HistorialAsignacionRepository;
import org.proyecto.data.OperadorRepository;
import org.proyecto.data.PrioridadRepository;
import org.proyecto.data.TicketEstadoRepository;
import org.proyecto.data.TicketRepository;
import org.proyecto.entities.Adjunto;
import org.proyecto.entities.Categoria;
import org.proyecto.entities.Comentario;
import org.proyecto.entities.HistorialAsignacion;
import org.proyecto.entities.HistorialEstado;
import org.proyecto.entities.Operador;
import org.proyecto.entities.Prioridad;
import org.proyecto.entities.Ticket;
import org.proyecto.entities.TicketEstado;
import org.proyecto.entities.TicketEstadoCodigo;
import org.proyecto.entities.TipoAccionAsignacion;
import org.proyecto.entities.TipoRegistroHistorial;
import org.proyecto.entities.UsuarioTicket;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

    private static final int LARGO_MAXIMO_TEXTO = 2000;

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
    private final LimitesAdjuntos limitesAdjuntos;
    private final ComentarioRepository comentarioRepository;

    public TicketService(TicketRepository ticketRepository, AdjuntoRepository adjuntoRepository,
                          HistorialAsignacionRepository historialAsignacionRepository,
                          TicketEstadoRepository ticketEstadoRepository, CategoriaRepository categoriaRepository,
                          PrioridadRepository prioridadRepository, OperadorRepository operadorRepository,
                          NotificacionService notificacionService, LimitesAdjuntos limitesAdjuntos,
                          ComentarioRepository comentarioRepository) {
        this.ticketRepository = ticketRepository;
        this.adjuntoRepository = adjuntoRepository;
        this.historialAsignacionRepository = historialAsignacionRepository;
        this.ticketEstadoRepository = ticketEstadoRepository;
        this.categoriaRepository = categoriaRepository;
        this.prioridadRepository = prioridadRepository;
        this.operadorRepository = operadorRepository;
        this.notificacionService = notificacionService;
        this.limitesAdjuntos = limitesAdjuntos;
        this.comentarioRepository = comentarioRepository;
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
        if (idCategoria == null) {
            throw new InvalidTicketOperationException("La categoría es obligatoria");
        }

        Ticket ticket = new Ticket();
        ticket.setTitulo(titulo);
        ticket.setDescripcion(descripcion);
        ticket.setUsuario(usuario);
        ticket.setFechaCreacion(LocalDateTime.now());
        ticket.setCategoria(buscarCategoriaActiva(idCategoria));
        if (idPrioridadSugerida != null) {
            ticket.setPrioridadSugerida(buscarPrioridad(idPrioridadSugerida));
        }

        aplicarEstado(ticket, TicketEstadoCodigo.NUEVO, usuario, null, "Registro del reclamo", null);
        ticket = ticketRepository.save(ticket);

        // Paso 12: aviso al área de operadores de la categoría.
        for (Operador operador : operadoresDelArea(ticket.getCategoria())) {
            notificacionService.notificarOperador(ticket, operador,
                    "Nuevo ticket " + numero(ticket) + ": " + ticket.getTitulo());
        }
        return ticket;
    }

    // ---- CU02 - Adjuntar archivo ----
    public Adjunto adjuntarArchivo(Long ticketId, Long usuarioId, String nombreArchivo,
                                    String tipoArchivo, long tamano, byte[] contenido, String descripcion) {
        Ticket ticket = buscarTicketPropio(ticketId, usuarioId);
        requireAdjuntosEditables(ticket);

        Adjunto adjunto = guardarAdjunto(ticket, nombreArchivo, tipoArchivo, tamano, contenido, descripcion, null);

        registrarEvento(ticket, ticket.getUsuario(), null, "Adjuntó el archivo " + nombreArchivo);
        ticketRepository.save(ticket);
        return adjunto;
    }

    /** FA3 de CU02: el titular elimina un adjunto mientras el ticket siga en un estado editable. */
    public Ticket eliminarAdjunto(Long ticketId, Long adjuntoId, Long usuarioId) {
        Ticket ticket = buscarTicketPropio(ticketId, usuarioId);
        requireAdjuntosEditables(ticket);

        Adjunto adjunto = adjuntoRepository.findById(adjuntoId)
                .filter(a -> a.getTicket().getId().equals(ticketId))
                .orElseThrow(() -> new RecursoNoEncontradoException("Adjunto inexistente: " + adjuntoId));
        if (adjunto.getOperadorAutor() != null) {
            throw new InvalidTicketOperationException("La evidencia que cargó el operador no se puede eliminar");
        }
        ticket.getAdjuntos().remove(adjunto);
        adjuntoRepository.delete(adjunto);

        registrarEvento(ticket, ticket.getUsuario(), null, "Eliminó el archivo " + adjunto.getNombreArchivo());
        return ticketRepository.save(ticket);
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
    public Ticket clasificar(Long ticketId, Long idCategoria, Operador operador) {
        Ticket ticket = buscarTicketExistente(ticketId);
        requireEstadoClasificable(ticket);

        Categoria anterior = ticket.getCategoria();
        Categoria categoria = buscarCategoriaActiva(idCategoria);
        ticket.setCategoria(categoria);
        // FA1: si ya tenía categoría queda como reclasificación.
        registrarEvento(ticket, null, operador, anterior == null
                ? "Clasificado en " + categoria.getNombre()
                : "Reclasificado de " + anterior.getNombre() + " a " + categoria.getNombre());
        return ticketRepository.save(ticket);
    }

    // ---- CU05 - Asignar prioridad ----
    public Ticket asignarPrioridad(Long ticketId, Long idPrioridad, Operador operador) {
        Ticket ticket = buscarTicketExistente(ticketId);
        requireEstadoClasificable(ticket);
        if (ticket.getCategoria() == null) {
            throw new InvalidTicketOperationException("El ticket debe estar clasificado antes de asignar prioridad");
        }

        Prioridad anterior = ticket.getPrioridad();
        Prioridad prioridad = buscarPrioridad(idPrioridad);
        ticket.setPrioridad(prioridad);
        // FA1: si ya tenía prioridad queda como repriorización.
        registrarEvento(ticket, null, operador, anterior == null
                ? "Prioridad asignada: " + prioridad.getNivel()
                : "Repriorizado de " + anterior.getNivel() + " a " + prioridad.getNivel());
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
        notificacionService.notificarUsuario(ticket, ticket.getUsuario(),
                "Tu ticket " + numero(ticket) + " necesita corrección: " + observacion);
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
        notificacionService.notificarOperador(ticket, nuevoOperador,
                "Se te asignó el ticket " + numero(ticket) + ": " + ticket.getTitulo());
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
        notificacionService.notificarOperador(ticket, operadorNuevo,
                "Se te reasignó el ticket " + numero(ticket) + ": " + motivo);
        notificacionService.notificarOperador(ticket, operadorAnterior,
                "El ticket " + numero(ticket) + " fue reasignado: " + motivo);
        return ticket;
    }

    public List<Ticket> listarAsignadosA(Long operadorId) {
        return ticketRepository.findByOperadorIdOrderByFechaCreacionDesc(operadorId);
    }

    public List<Ticket> listarSinAsignar() {
        return ticketRepository.findByEstadoActual_CodigoAndOperadorIsNullOrderByFechaCreacionAsc(
                TicketEstadoCodigo.VALIDADO);
    }

    // ---- CU07 - Actualizar estado del ticket ----

    /**
     * Estados a los que CU07 deja pasar al operador y el permiso que exige cada uno. Las demás
     * transiciones (validación, asignación, corrección) tienen su propio caso de uso.
     */
    private static final Map<TicketEstadoCodigo, String> PERMISO_POR_DESTINO = Map.of(
            TicketEstadoCodigo.RESUELTO, Permisos.TICKET_RESOLVER,
            TicketEstadoCodigo.CERRADO, Permisos.TICKET_RESOLVER,
            TicketEstadoCodigo.REABIERTO, Permisos.TICKET_REABRIR_OPERATIVO);

    /** Paso 2: estados posibles desde el actual para este operador (vacío si no le corresponde operar). */
    public List<TicketEstadoCodigo> transicionesDisponibles(Ticket ticket, Operador operador, Set<String> permisos) {
        boolean gestionaTodos = permisos.contains(Permisos.TICKET_GESTIONAR_TODOS);
        if (operador == null || ticket.getOperador() == null || (!gestionaTodos && !esOperadorAsignado(ticket, operador))) {
            return List.of();
        }
        return ticket.getEstadoActual().getCodigo().siguientes().stream()
                .filter(PERMISO_POR_DESTINO::containsKey)
                .filter(destino -> gestionaTodos || permisos.contains(PERMISO_POR_DESTINO.get(destino)))
                .sorted()
                .toList();
    }

    /**
     * Valida la transición contra la máquina de estados (FA1) y delega en el caso de uso del estado
     * destino, que exige su texto (FA2): resolución (CU09), solución (CU17) o motivo (CU19).
     */
    public Ticket actualizarEstado(Long ticketId, TicketEstadoCodigo destino, String observacion,
                                   Operador operador, Set<String> permisos) {
        if (destino == null) {
            throw new InvalidTicketOperationException("Debe seleccionar el nuevo estado");
        }
        Ticket ticket = buscarTicketExistente(ticketId);
        List<TicketEstadoCodigo> posibles = transicionesDisponibles(ticket, operador, permisos);
        if (!posibles.contains(destino)) {
            throw new InvalidTicketOperationException("No se puede pasar de " + ticket.getEstadoActual().getCodigo()
                    + " a " + destino + ". " + (posibles.isEmpty()
                    ? "No tenés cambios de estado disponibles para este ticket."
                    : "Estados posibles: " + posibles.stream().map(Enum::name).collect(Collectors.joining(", "))));
        }

        boolean gestionaTodos = permisos.contains(Permisos.TICKET_GESTIONAR_TODOS);
        return switch (destino) {
            case RESUELTO -> resolver(ticketId, operador, observacion, gestionaTodos);
            case CERRADO -> cerrarTicket(ticketId, operador, observacion, gestionaTodos);
            case REABIERTO -> reabrirTicketPorOperador(ticketId, operador, observacion, gestionaTodos);
            default -> throw new IllegalStateException("Destino sin caso de uso: " + destino);
        };
    }

    // ---- CU08 - Registrar acciones sobre ticket ----
    public boolean puedeRegistrarAccion(Ticket ticket, Operador operador) {
        return operador != null && esOperadorAsignado(ticket, operador)
                && ticket.getEstadoActual().getCodigo() != TicketEstadoCodigo.CERRADO;
    }

    /** nombreArchivo null = acción sin adjunto; con archivo es el FA2 (evidencia, mismas reglas que CU02). */
    public Ticket registrarAccion(Long ticketId, Operador operador, String descripcion, String nombreArchivo,
                                  String tipoArchivo, long tamano, byte[] contenido) {
        requireTexto(descripcion, "La descripción de la acción es obligatoria");
        Ticket ticket = buscarTicketExistente(ticketId);
        if (!esOperadorAsignado(ticket, operador)) {
            throw new InvalidTicketOperationException("Solo el operador asignado puede registrar acciones sobre el ticket");
        }
        if (!puedeRegistrarAccion(ticket, operador)) {
            throw new InvalidTicketOperationException("No se pueden registrar acciones sobre un ticket cerrado");
        }

        String detalleAdjunto = null;
        if (nombreArchivo != null) {
            guardarAdjunto(ticket, nombreArchivo, tipoArchivo, tamano, contenido, descripcion, operador);
            detalleAdjunto = "Evidencia adjunta: " + nombreArchivo;
        }
        agregarHistorial(ticket, TipoRegistroHistorial.ACCION, null, operador, descripcion, detalleAdjunto);
        return ticketRepository.save(ticket);
    }

    // ---- CU09 - Resolver ticket ----
    public Ticket resolver(Long ticketId, Operador operador, String descripcion, boolean gestionaTodos) {
        Ticket ticket = buscarTicketExistente(ticketId);

        if (!gestionaTodos && !esOperadorAsignado(ticket, operador)) {
            throw new InvalidTicketOperationException("Solo el operador asignado puede resolver el ticket");
        }
        if (!ticket.getEstadoActual().getCodigo().permiteTransicionA(TicketEstadoCodigo.RESUELTO)) {
            throw new InvalidTicketOperationException(
                    "El ticket no se puede marcar como resuelto desde su estado actual (" + ticket.getEstadoActual().getNombre() + ")");
        }
        requireTexto(descripcion, "La descripción de la resolución es obligatoria");

        ticket.setResolucion(descripcion);
        aplicarEstado(ticket, TicketEstadoCodigo.RESUELTO, null, operador, "Ticket resuelto", descripcion);
        ticket = ticketRepository.save(ticket);
        notificacionService.notificarUsuario(ticket, ticket.getUsuario(),
                "Tu ticket " + numero(ticket) + " fue resuelto: " + descripcion);
        return ticket;
    }

    // ---- CU15 - Agregar comentario ----
    public List<Comentario> listarComentarios(Long ticketId) {
        return comentarioRepository.findByTicketIdOrderByFechaCreacionAsc(ticketId);
    }

    /** Precondiciones 2 y 3: titular u operador asignado, con el ticket fuera de CERRADO. */
    public boolean puedeComentar(Ticket ticket, UsuarioTicket usuario, Operador operador) {
        if (ticket.getEstadoActual().getCodigo() == TicketEstadoCodigo.CERRADO) {
            return false;
        }
        if (usuario != null) {
            return ticket.getUsuario().getId().equals(usuario.getId());
        }
        return operador != null && esOperadorAsignado(ticket, operador);
    }

    /** Exactamente uno de los dos autores: el titular (usuario) o el operador asignado. */
    public Ticket agregarComentario(Long ticketId, UsuarioTicket autorUsuario, Operador autorOperador, String texto) {
        requireTexto(texto, "El comentario no puede estar vacío");
        Ticket ticket = autorUsuario != null
                ? buscarTicketPropio(ticketId, autorUsuario.getId())
                : buscarTicketExistente(ticketId);
        if (autorUsuario == null && !esOperadorAsignado(ticket, autorOperador)) {
            throw new InvalidTicketOperationException("Solo el operador asignado puede comentar el ticket");
        }
        if (!puedeComentar(ticket, autorUsuario, autorOperador)) {
            throw new InvalidTicketOperationException("No se puede comentar un ticket cerrado");
        }

        Comentario comentario = new Comentario();
        comentario.setTicket(ticket);
        comentario.setContenido(texto);
        comentario.setFechaCreacion(LocalDateTime.now());
        comentario.setUsuarioAutor(autorUsuario);
        comentario.setOperadorAutor(autorOperador);
        comentarioRepository.save(comentario);
        ticket.setFechaUltimaActualizacion(LocalDateTime.now());
        ticket = ticketRepository.save(ticket);

        // Paso 8: aviso a la contraparte. FA2: sin operador asignado no hay a quién avisar.
        if (autorUsuario != null && ticket.getOperador() != null) {
            notificacionService.notificarOperador(ticket, ticket.getOperador(),
                    "Nuevo comentario del usuario en el ticket " + numero(ticket) + ": " + texto);
        } else if (autorOperador != null) {
            notificacionService.notificarUsuario(ticket, ticket.getUsuario(),
                    "Nuevo comentario del operador en tu ticket " + numero(ticket) + ": " + texto);
        }
        return ticket;
    }

    // ---- CU17 - Cerrar ticket ----
    public Ticket cerrarTicket(Long ticketId, Operador operador, String motivoCierre, boolean gestionaTodos) {
        Ticket ticket = buscarTicketExistente(ticketId);

        // Precondición 3: operador asignado o con permisos de administración.
        if (!gestionaTodos && !esOperadorAsignado(ticket, operador)) {
            throw new InvalidTicketOperationException("Solo el operador asignado puede cerrar el ticket");
        }
        if (ticket.getEstadoActual().getCodigo() != TicketEstadoCodigo.RESUELTO) {
            throw new InvalidTicketOperationException("El ticket debe estar RESUELTO para poder cerrarlo");
        }
        requireTexto(motivoCierre, "La descripción de la solución es obligatoria");

        ticket.setMotivoCierre(motivoCierre);
        ticket.setFechaCierre(LocalDateTime.now());
        aplicarEstado(ticket, TicketEstadoCodigo.CERRADO, null, operador, "Cierre del ticket", motivoCierre);
        ticket = ticketRepository.save(ticket);
        notificacionService.notificarUsuario(ticket, ticket.getUsuario(),
                "Tu ticket " + numero(ticket) + " fue cerrado. Solución: " + motivoCierre);
        return ticket;
    }

    // ---- CU18 - Validar resolución ----
    public Ticket validarResolucion(Long ticketId, UsuarioTicket usuario) {
        Ticket ticket = buscarTicketPropio(ticketId, usuario.getId());

        if (ticket.getEstadoActual().getCodigo() != TicketEstadoCodigo.CERRADO) {
            throw new InvalidTicketOperationException("El ticket debe estar CERRADO para poder validarlo");
        }

        ticket.setValidado(true);
        registrarEvento(ticket, usuario, null, "El usuario confirmó la resolución");
        ticket = ticketRepository.save(ticket);
        if (ticket.getOperador() != null) {
            notificacionService.notificarOperador(ticket, ticket.getOperador(),
                    "El usuario validó la resolución del ticket " + numero(ticket));
        }
        return ticket;
    }

    // ---- CU19 - Reabrir ticket ----
    public Ticket reabrirTicketPorUsuario(Long ticketId, UsuarioTicket usuario, String motivo) {
        Ticket ticket = buscarTicketPropio(ticketId, usuario.getId());
        return reabrir(ticket, usuario, null, motivo);
    }

    public Ticket reabrirTicketPorOperador(Long ticketId, Operador operador, String motivo, boolean gestionaTodos) {
        Ticket ticket = buscarTicketExistente(ticketId);
        if (!gestionaTodos && !esOperadorAsignado(ticket, operador)) {
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
        requireTexto(motivo, "El motivo de la reapertura es obligatorio");

        ticket.setMotivoReapertura(motivo);
        ticket.setValidado(false);
        aplicarEstado(ticket, TicketEstadoCodigo.REABIERTO, actorUsuario, actorOperador, motivo, null);
        ticket = ticketRepository.save(ticket);

        if (ticket.getOperador() != null) {
            notificacionService.notificarOperador(ticket, ticket.getOperador(),
                    "El ticket " + numero(ticket) + " fue reabierto: " + motivo);
        }
        if (actorOperador != null) {
            notificacionService.notificarUsuario(ticket, ticket.getUsuario(),
                    "El ticket " + numero(ticket) + " fue reabierto por el operador: " + motivo);
        }
        return ticket;
    }

    // ---- Helpers ----

    private HistorialEstado aplicarEstado(Ticket ticket, TicketEstadoCodigo codigo, UsuarioTicket actorUsuario,
                                           Operador actorOperador, String observacion, String motivoCambio) {
        TicketEstado actual = ticket.getEstadoActual();
        if (actual != null && actual.getCodigo() != codigo && !actual.getCodigo().permiteTransicionA(codigo)) {
            throw new InvalidTicketOperationException(
                    "Transición no permitida de " + actual.getCodigo() + " a " + codigo);
        }
        TicketEstado estado = ticketEstadoRepository.findByCodigo(codigo)
                .orElseThrow(() -> new IllegalStateException("Estado no seedeado: " + codigo));
        ticket.setEstadoActual(estado);
        return agregarHistorial(ticket, TipoRegistroHistorial.CAMBIO_ESTADO, actorUsuario, actorOperador,
                observacion, motivoCambio);
    }

    /**
     * Deja constancia en el historial de una acción que no cambia el estado (clasificación,
     * priorización, adjunto, validación de resolución): la entrada repite el estado vigente.
     */
    private HistorialEstado registrarEvento(Ticket ticket, UsuarioTicket actorUsuario, Operador actorOperador,
                                             String observacion) {
        return agregarHistorial(ticket, TipoRegistroHistorial.ACTUALIZACION, actorUsuario, actorOperador,
                observacion, null);
    }

    private HistorialEstado agregarHistorial(Ticket ticket, TipoRegistroHistorial tipo, UsuarioTicket actorUsuario,
                                              Operador actorOperador, String observacion, String motivoCambio) {
        ticket.setFechaUltimaActualizacion(LocalDateTime.now());

        HistorialEstado historial = new HistorialEstado();
        historial.setTipo(tipo);
        historial.setTicket(ticket);
        historial.setTicketEstado(ticket.getEstadoActual());
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

    /** Valida formato, tamaño y cantidad (CU02 FA1, FA2, E2) y guarda el archivo; operadorAutor solo en CU08. */
    private Adjunto guardarAdjunto(Ticket ticket, String nombreArchivo, String tipoArchivo, long tamano,
                                   byte[] contenido, String descripcion, Operador operadorAutor) {
        if (!tieneExtensionPermitida(nombreArchivo)) {
            throw new InvalidTicketOperationException(
                    "Formato de archivo no permitido. Formatos válidos: JPG, PNG, PDF, DOCX, XLSX");
        }
        if (tamano > limitesAdjuntos.getTamanioMaximoBytes()) {
            throw new InvalidTicketOperationException(
                    "El archivo supera el tamaño máximo permitido (" + limitesAdjuntos.getTamanioMaximoMb() + " MB)");
        }
        if (adjuntoRepository.countByTicketId(ticket.getId()) >= limitesAdjuntos.getMaximoPorTicket()) {
            throw new InvalidTicketOperationException(
                    "El ticket ya tiene el máximo de " + limitesAdjuntos.getMaximoPorTicket() + " adjuntos permitidos");
        }

        Adjunto adjunto = new Adjunto();
        adjunto.setTicket(ticket);
        adjunto.setNombreArchivo(nombreArchivo);
        adjunto.setTipoArchivo(tipoArchivo);
        adjunto.setTamano(tamano);
        adjunto.setDescripcion(descripcion);
        adjunto.setFechaCarga(LocalDateTime.now());
        adjunto.setContenido(contenido);
        adjunto.setOperadorAutor(operadorAutor);
        return adjuntoRepository.save(adjunto);
    }

    private void requireTexto(String texto, String mensajeSiVacio) {
        if (texto == null || texto.isBlank()) {
            throw new InvalidTicketOperationException(mensajeSiVacio);
        }
        if (texto.length() > LARGO_MAXIMO_TEXTO) {
            throw new InvalidTicketOperationException("El texto no puede superar los " + LARGO_MAXIMO_TEXTO + " caracteres");
        }
    }

    private void requireAdjuntosEditables(Ticket ticket) {
        if (!ESTADOS_EDITABLES_ADJUNTOS.contains(ticket.getEstadoActual().getCodigo())) {
            throw new InvalidTicketOperationException(
                    "El ticket no admite cambios en sus adjuntos en su estado actual (" + ticket.getEstadoActual().getNombre() + ")");
        }
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

    private Categoria buscarCategoriaActiva(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new InvalidTicketOperationException("Categoría inexistente: " + id));
        if (!categoria.isActiva()) {
            throw new InvalidTicketOperationException("La categoría seleccionada no está activa");
        }
        return categoria;
    }

    /** Operadores activos del área de la categoría; si el área no tiene ninguno, todos los activos. */
    private List<Operador> operadoresDelArea(Categoria categoria) {
        List<Operador> activos = operadorRepository.findByActivoTrue();
        String area = categoria.getArea();
        if (area == null || area.isBlank()) {
            return activos;
        }
        List<Operador> delArea = activos.stream()
                .filter(o -> o.getArea() != null && o.getArea().trim().equalsIgnoreCase(area.trim()))
                .toList();
        return delArea.isEmpty() ? activos : delArea;
    }

    /** Número de ticket tal como lo muestra la interfaz (TK-000034). */
    private static String numero(Ticket ticket) {
        return String.format("TK-%06d", ticket.getId());
    }

    private boolean esOperadorAsignado(Ticket ticket, Operador operador) {
        return ticket.getOperador() != null && ticket.getOperador().getId().equals(operador.getId());
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
        return LimitesAdjuntos.EXTENSIONES_PERMITIDAS.contains(extension);
    }
}

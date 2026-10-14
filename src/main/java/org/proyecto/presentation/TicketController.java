package org.proyecto.presentation;

import org.proyecto.business.Autorizador;
import org.proyecto.business.ForbiddenOperationException;
import org.proyecto.business.InvalidTicketOperationException;
import org.proyecto.business.LimitesAdjuntos;
import org.proyecto.business.Permisos;
import org.proyecto.business.SesionActiva;
import org.proyecto.business.TicketService;
import org.proyecto.config.AuthenticatedSession;
import org.proyecto.entities.Adjunto;
import org.proyecto.entities.Operador;
import org.proyecto.entities.Ticket;
import org.proyecto.entities.UsuarioTicket;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final Autorizador autorizador;
    private final LimitesAdjuntos limitesAdjuntos;

    public TicketController(TicketService ticketService, Autorizador autorizador, LimitesAdjuntos limitesAdjuntos) {
        this.ticketService = ticketService;
        this.autorizador = autorizador;
        this.limitesAdjuntos = limitesAdjuntos;
    }

    // CU01
    @PostMapping
    public ResponseEntity<TicketResponse> registrar(@AuthenticatedSession SesionActiva sesion,
                                                      @RequestBody CrearTicketRequest request) {
        UsuarioTicket usuario = usuarioDe(sesion, Permisos.TICKET_CREAR);
        Ticket ticket = ticketService.registrarTicket(usuario, request.getTitulo(), request.getDescripcion(),
                request.getIdCategoria(), request.getIdPrioridadSugerida());
        return ResponseEntity.ok(enriquecer(ticket, sesion));
    }

    // CU02
    @PostMapping("/{id}/adjuntos")
    public ResponseEntity<AdjuntoResponse> adjuntar(@AuthenticatedSession SesionActiva sesion, @PathVariable Long id,
                                                      @RequestParam("file") MultipartFile file) {
        UsuarioTicket usuario = usuarioDe(sesion, Permisos.TICKET_CREAR);
        if (file.isEmpty() || file.getOriginalFilename() == null) {
            throw new InvalidTicketOperationException("El archivo es obligatorio");
        }

        Adjunto adjunto = ticketService.adjuntarArchivo(
                id, usuario.getId(), file.getOriginalFilename(), file.getContentType(),
                file.getSize(), leerContenido(file), null);

        return ResponseEntity.ok(AdjuntoResponse.from(adjunto));
    }

    // CU02 FA3
    @DeleteMapping("/{id}/adjuntos/{idAdjunto}")
    public ResponseEntity<TicketResponse> eliminarAdjunto(@AuthenticatedSession SesionActiva sesion, @PathVariable Long id,
                                                            @PathVariable Long idAdjunto) {
        UsuarioTicket usuario = usuarioDe(sesion, Permisos.TICKET_CREAR);
        Ticket ticket = ticketService.eliminarAdjunto(id, idAdjunto, usuario.getId());
        return ResponseEntity.ok(enriquecer(ticket, sesion));
    }

    @GetMapping("/adjuntos/limites")
    public LimitesAdjuntosResponse limitesAdjuntos(@AuthenticatedSession SesionActiva sesion) {
        autorizador.requirePermiso(sesion, Permisos.TICKET_CREAR);
        return LimitesAdjuntosResponse.from(limitesAdjuntos);
    }

    // CU03
    @GetMapping("/mios")
    public List<TicketResponse> misTickets(@AuthenticatedSession SesionActiva sesion) {
        UsuarioTicket usuario = usuarioDe(sesion, Permisos.TICKET_VER_PROPIOS);
        return ticketService.consultarTicketsDeUsuario(usuario.getId()).stream()
                .map(TicketResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> detalle(@AuthenticatedSession SesionActiva sesion, @PathVariable Long id) {
        Ticket ticket = ticketService.obtenerDetalle(id);
        if (!sesion.tiene(Permisos.TICKET_VER_TODOS)) {
            UsuarioTicket usuario = usuarioDe(sesion, Permisos.TICKET_VER_PROPIOS);
            if (!ticket.getUsuario().getId().equals(usuario.getId())) {
                throw new ForbiddenOperationException("No podés ver un ticket que no te pertenece");
            }
        }
        return ResponseEntity.ok(enriquecer(ticket, sesion));
    }

    // CU06 FA2 - usuario reenvía tras PENDIENTE_CORRECCION
    @PostMapping("/{id}/reenviar")
    public ResponseEntity<TicketResponse> reenviar(@AuthenticatedSession SesionActiva sesion, @PathVariable Long id,
                                                     @RequestBody ReenviarTicketRequest request) {
        UsuarioTicket usuario = usuarioDe(sesion, Permisos.TICKET_REENVIAR);
        Ticket ticket = ticketService.reenviarTicket(id, usuario.getId(), request.getTitulo(), request.getDescripcion());
        return ResponseEntity.ok(enriquecer(ticket, sesion));
    }

    // CU04
    @GetMapping("/pendientes-clasificacion")
    public List<TicketResponse> pendientesClasificacion(@AuthenticatedSession SesionActiva sesion) {
        autorizador.requirePermiso(sesion, Permisos.TICKET_CLASIFICAR);
        return ticketService.listarPendientesClasificacion().stream().map(TicketResponse::from).toList();
    }

    @PostMapping("/{id}/clasificar")
    public ResponseEntity<TicketResponse> clasificar(@AuthenticatedSession SesionActiva sesion, @PathVariable Long id,
                                                       @RequestBody ClasificarTicketRequest request) {
        Operador operador = operadorDe(sesion, Permisos.TICKET_CLASIFICAR);
        Ticket ticket = ticketService.clasificar(id, request.getIdCategoria(), operador);
        return ResponseEntity.ok(enriquecer(ticket, sesion));
    }

    // CU05
    @PostMapping("/{id}/prioridad")
    public ResponseEntity<TicketResponse> asignarPrioridad(@AuthenticatedSession SesionActiva sesion, @PathVariable Long id,
                                                             @RequestBody AsignarPrioridadRequest request) {
        Operador operador = operadorDe(sesion, Permisos.TICKET_CLASIFICAR);
        Ticket ticket = ticketService.asignarPrioridad(id, request.getIdPrioridad(), operador);
        return ResponseEntity.ok(enriquecer(ticket, sesion));
    }

    // CU06
    @PostMapping("/{id}/validar")
    public ResponseEntity<TicketResponse> validar(@AuthenticatedSession SesionActiva sesion, @PathVariable Long id) {
        Operador operador = operadorDe(sesion, Permisos.TICKET_CLASIFICAR);
        Ticket ticket = ticketService.validarTicket(id, operador);
        return ResponseEntity.ok(enriquecer(ticket, sesion));
    }

    @PostMapping("/{id}/marcar-incompleto")
    public ResponseEntity<TicketResponse> marcarIncompleto(@AuthenticatedSession SesionActiva sesion, @PathVariable Long id,
                                                             @RequestBody MarcarIncompletoRequest request) {
        Operador operador = operadorDe(sesion, Permisos.TICKET_CLASIFICAR);
        Ticket ticket = ticketService.marcarIncompleto(id, operador, request.getObservacion());
        return ResponseEntity.ok(enriquecer(ticket, sesion));
    }

    @GetMapping("/asignados-a-mi")
    public List<TicketResponse> asignadosAMi(@AuthenticatedSession SesionActiva sesion) {
        Operador operador = operadorDe(sesion, Permisos.TICKET_RESOLVER);
        return ticketService.listarAsignadosA(operador.getId()).stream().map(TicketResponse::from).toList();
    }

    // CU13
    @GetMapping("/sin-asignar")
    public List<TicketResponse> sinAsignar(@AuthenticatedSession SesionActiva sesion) {
        autorizador.requirePermiso(sesion, Permisos.TICKET_ASIGNAR);
        return ticketService.listarSinAsignar().stream().map(TicketResponse::from).toList();
    }

    @PostMapping("/{id}/asignar")
    public ResponseEntity<TicketResponse> asignar(@AuthenticatedSession SesionActiva sesion, @PathVariable Long id,
                                                    @RequestBody AsignarOperadorRequest request) {
        Operador responsable = operadorDe(sesion, Permisos.TICKET_ASIGNAR);
        Ticket ticket = ticketService.asignarOperador(id, request.getIdOperador(), responsable);
        return ResponseEntity.ok(enriquecer(ticket, sesion));
    }

    // CU14
    @PostMapping("/{id}/reasignar")
    public ResponseEntity<TicketResponse> reasignar(@AuthenticatedSession SesionActiva sesion, @PathVariable Long id,
                                                      @RequestBody ReasignarOperadorRequest request) {
        Operador responsable = operadorDe(sesion, Permisos.TICKET_ASIGNAR);
        Ticket ticket = ticketService.reasignarOperador(id, request.getIdOperadorNuevo(), request.getMotivo(), responsable);
        return ResponseEntity.ok(enriquecer(ticket, sesion));
    }

    // CU07
    @PostMapping("/{id}/estado")
    public ResponseEntity<TicketResponse> actualizarEstado(@AuthenticatedSession SesionActiva sesion, @PathVariable Long id,
                                                             @RequestBody ActualizarEstadoRequest request) {
        Operador operador = operadorDe(sesion, Permisos.TICKET_RESOLVER);
        Ticket ticket = ticketService.actualizarEstado(id, request.getEstado(), request.getObservacion(),
                operador, sesion.permisos());
        return ResponseEntity.ok(enriquecer(ticket, sesion));
    }

    // CU08 (con archivo opcional: FA2)
    @PostMapping("/{id}/acciones")
    public ResponseEntity<TicketResponse> registrarAccion(@AuthenticatedSession SesionActiva sesion, @PathVariable Long id,
                                                            @RequestParam("descripcion") String descripcion,
                                                            @RequestParam(value = "file", required = false) MultipartFile file) {
        Operador operador = operadorDe(sesion, Permisos.TICKET_RESOLVER);
        boolean conArchivo = file != null && !file.isEmpty() && file.getOriginalFilename() != null;
        Ticket ticket = ticketService.registrarAccion(id, operador, descripcion,
                conArchivo ? file.getOriginalFilename() : null,
                conArchivo ? file.getContentType() : null,
                conArchivo ? file.getSize() : 0,
                conArchivo ? leerContenido(file) : null);
        return ResponseEntity.ok(enriquecer(ticket, sesion));
    }

    // CU15: comenta el titular o, si la cuenta es de un operador, el operador asignado.
    @PostMapping("/{id}/comentarios")
    public ResponseEntity<TicketResponse> comentar(@AuthenticatedSession SesionActiva sesion, @PathVariable Long id,
                                                     @RequestBody AgregarComentarioRequest request) {
        UsuarioTicket usuario = usuarioTitular(sesion);
        Ticket ticket = usuario != null
                ? ticketService.agregarComentario(id, usuario, null, request.getContenido())
                : ticketService.agregarComentario(id, null, operadorDe(sesion, Permisos.TICKET_RESOLVER), request.getContenido());
        return ResponseEntity.ok(enriquecer(ticket, sesion));
    }

    // CU09
    @PostMapping("/{id}/resolver")
    public ResponseEntity<TicketResponse> resolver(@AuthenticatedSession SesionActiva sesion, @PathVariable Long id,
                                                     @RequestBody ResolverTicketRequest request) {
        Operador operador = operadorDe(sesion, Permisos.TICKET_RESOLVER);
        Ticket ticket = ticketService.resolver(id, operador, request.getDescripcion(),
                sesion.tiene(Permisos.TICKET_GESTIONAR_TODOS));
        return ResponseEntity.ok(enriquecer(ticket, sesion));
    }

    // CU17
    @PostMapping("/{id}/cerrar")
    public ResponseEntity<TicketResponse> cerrar(@AuthenticatedSession SesionActiva sesion, @PathVariable Long id,
                                                   @RequestBody CerrarTicketRequest request) {
        Operador operador = operadorDe(sesion, Permisos.TICKET_RESOLVER);
        Ticket ticket = ticketService.cerrarTicket(id, operador, request.getSolucion(),
                sesion.tiene(Permisos.TICKET_GESTIONAR_TODOS));
        return ResponseEntity.ok(enriquecer(ticket, sesion));
    }

    // CU18
    @PostMapping("/{id}/validar-resolucion")
    public ResponseEntity<TicketResponse> validarResolucion(@AuthenticatedSession SesionActiva sesion, @PathVariable Long id) {
        UsuarioTicket usuario = usuarioDe(sesion, Permisos.TICKET_VALIDAR_RESOLUCION);
        Ticket ticket = ticketService.validarResolucion(id, usuario);
        return ResponseEntity.ok(enriquecer(ticket, sesion));
    }

    // CU19
    @PostMapping("/{id}/reabrir")
    public ResponseEntity<TicketResponse> reabrir(@AuthenticatedSession SesionActiva sesion, @PathVariable Long id,
                                                    @RequestBody ReabrirTicketRequest request) {
        Ticket ticket = sesion.tiene(Permisos.TICKET_REABRIR_PROPIO)
                ? ticketService.reabrirTicketPorUsuario(id, usuarioDe(sesion, Permisos.TICKET_REABRIR_PROPIO), request.getMotivo())
                : ticketService.reabrirTicketPorOperador(id, operadorDe(sesion, Permisos.TICKET_REABRIR_OPERATIVO),
                        request.getMotivo(), sesion.tiene(Permisos.TICKET_GESTIONAR_TODOS));
        return ResponseEntity.ok(enriquecer(ticket, sesion));
    }

    private TicketResponse enriquecer(Ticket ticket, SesionActiva sesion) {
        TicketResponse response = TicketResponse.from(ticket);
        Operador operador = sesion.cuenta().getOperador();
        response.setTransicionesDisponibles(ticketService.transicionesDisponibles(ticket, operador, sesion.permisos()));
        response.setPuedeRegistrarAccion(ticketService.puedeRegistrarAccion(ticket, operador));
        response.setComentarios(ticketService.listarComentarios(ticket.getId()).stream()
                .map(ComentarioResponse::from).toList());
        response.setPuedeComentar(ticketService.puedeComentar(ticket, usuarioTitular(sesion),
                sesion.tiene(Permisos.TICKET_RESOLVER) ? operador : null));
        response.setAdjuntos(ticketService.listarAdjuntos(ticket.getId()).stream().map(AdjuntoResponse::from).toList());
        response.setHistorialEstados(ticket.getHistorialEstados().stream().map(HistorialEstadoResponse::from).toList());
        response.setHistorialAsignaciones(ticketService.listarHistorialAsignaciones(ticket.getId()).stream()
                .map(HistorialAsignacionResponse::from).toList());
        return response;
    }

    /**
     * Separa autorización de identidad: primero se exige el permiso y después se obtiene la entidad
     * vinculada a la cuenta. Si alguien asignara un permiso de usuario a una cuenta de operador (o
     * al revés) la operación sigue rechazándose, porque la cuenta no tiene con quién actuar.
     */
    private UsuarioTicket usuarioDe(SesionActiva sesion, String permiso) {
        autorizador.requirePermiso(sesion, permiso);
        UsuarioTicket usuario = sesion.cuenta().getUsuarioTicket();
        if (usuario == null) {
            throw new ForbiddenOperationException("La cuenta no está vinculada a un usuario de tickets");
        }
        return usuario;
    }

    /** Usuario de tickets de la cuenta si puede ver sus propios tickets; null para cuentas de operador. */
    private UsuarioTicket usuarioTitular(SesionActiva sesion) {
        return sesion.tiene(Permisos.TICKET_VER_PROPIOS) ? sesion.cuenta().getUsuarioTicket() : null;
    }

    private Operador operadorDe(SesionActiva sesion, String permiso) {
        autorizador.requirePermiso(sesion, permiso);
        Operador operador = sesion.cuenta().getOperador();
        if (operador == null) {
            throw new ForbiddenOperationException("La cuenta no está vinculada a un operador");
        }
        return operador;
    }

    private byte[] leerContenido(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el archivo adjunto", e);
        }
    }
}

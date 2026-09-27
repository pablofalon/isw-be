package org.proyecto.presentation;

import org.proyecto.business.Autorizador;
import org.proyecto.business.ForbiddenOperationException;
import org.proyecto.business.InvalidTicketOperationException;
import org.proyecto.business.TicketService;
import org.proyecto.config.AuthenticatedSession;
import org.proyecto.entities.Adjunto;
import org.proyecto.entities.CuentaAcceso;
import org.proyecto.entities.Operador;
import org.proyecto.entities.Rol;
import org.proyecto.entities.Ticket;
import org.proyecto.entities.UsuarioTicket;
import org.springframework.http.ResponseEntity;
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

    public TicketController(TicketService ticketService, Autorizador autorizador) {
        this.ticketService = ticketService;
        this.autorizador = autorizador;
    }

    // CU01
    @PostMapping
    public ResponseEntity<TicketResponse> registrar(@AuthenticatedSession CuentaAcceso cuenta,
                                                      @RequestBody CrearTicketRequest request) {
        UsuarioTicket usuario = usuarioDe(cuenta);
        Ticket ticket = ticketService.registrarTicket(usuario, request.getTitulo(), request.getDescripcion(),
                request.getIdCategoria(), request.getIdPrioridadSugerida());
        return ResponseEntity.ok(enriquecer(ticket));
    }

    // CU02
    @PostMapping("/{id}/adjuntos")
    public ResponseEntity<AdjuntoResponse> adjuntar(@AuthenticatedSession CuentaAcceso cuenta, @PathVariable Long id,
                                                      @RequestParam("file") MultipartFile file) {
        UsuarioTicket usuario = usuarioDe(cuenta);
        if (file.isEmpty() || file.getOriginalFilename() == null) {
            throw new InvalidTicketOperationException("El archivo es obligatorio");
        }

        Adjunto adjunto = ticketService.adjuntarArchivo(
                id, usuario.getId(), file.getOriginalFilename(), file.getContentType(),
                file.getSize(), leerContenido(file), null);

        return ResponseEntity.ok(AdjuntoResponse.from(adjunto));
    }

    // CU03
    @GetMapping("/mios")
    public List<TicketResponse> misTickets(@AuthenticatedSession CuentaAcceso cuenta) {
        UsuarioTicket usuario = usuarioDe(cuenta);
        return ticketService.consultarTicketsDeUsuario(usuario.getId()).stream()
                .map(TicketResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> detalle(@AuthenticatedSession CuentaAcceso cuenta, @PathVariable Long id) {
        Ticket ticket = ticketService.obtenerDetalle(id);
        if (cuenta.getRol() == Rol.USUARIO && !ticket.getUsuario().getId().equals(cuenta.getUsuarioTicket().getId())) {
            throw new ForbiddenOperationException("No podés ver un ticket que no te pertenece");
        }
        return ResponseEntity.ok(enriquecer(ticket));
    }

    // CU06 FA2 - usuario reenvía tras PENDIENTE_CORRECCION
    @PostMapping("/{id}/reenviar")
    public ResponseEntity<TicketResponse> reenviar(@AuthenticatedSession CuentaAcceso cuenta, @PathVariable Long id,
                                                     @RequestBody ReenviarTicketRequest request) {
        UsuarioTicket usuario = usuarioDe(cuenta);
        Ticket ticket = ticketService.reenviarTicket(id, usuario.getId(), request.getTitulo(), request.getDescripcion());
        return ResponseEntity.ok(enriquecer(ticket));
    }

    // CU04
    @GetMapping("/pendientes-clasificacion")
    public List<TicketResponse> pendientesClasificacion(@AuthenticatedSession CuentaAcceso cuenta) {
        autorizador.requireRol(cuenta, Rol.OPERADOR, Rol.ADMINISTRADOR);
        return ticketService.listarPendientesClasificacion().stream().map(TicketResponse::from).toList();
    }

    @PostMapping("/{id}/clasificar")
    public ResponseEntity<TicketResponse> clasificar(@AuthenticatedSession CuentaAcceso cuenta, @PathVariable Long id,
                                                       @RequestBody ClasificarTicketRequest request) {
        autorizador.requireRol(cuenta, Rol.OPERADOR, Rol.ADMINISTRADOR);
        Ticket ticket = ticketService.clasificar(id, request.getIdCategoria());
        return ResponseEntity.ok(enriquecer(ticket));
    }

    // CU05
    @PostMapping("/{id}/prioridad")
    public ResponseEntity<TicketResponse> asignarPrioridad(@AuthenticatedSession CuentaAcceso cuenta, @PathVariable Long id,
                                                             @RequestBody AsignarPrioridadRequest request) {
        autorizador.requireRol(cuenta, Rol.OPERADOR, Rol.ADMINISTRADOR);
        Ticket ticket = ticketService.asignarPrioridad(id, request.getIdPrioridad());
        return ResponseEntity.ok(enriquecer(ticket));
    }

    // CU06
    @PostMapping("/{id}/validar")
    public ResponseEntity<TicketResponse> validar(@AuthenticatedSession CuentaAcceso cuenta, @PathVariable Long id) {
        Operador operador = operadorDe(cuenta);
        Ticket ticket = ticketService.validarTicket(id, operador);
        return ResponseEntity.ok(enriquecer(ticket));
    }

    @PostMapping("/{id}/marcar-incompleto")
    public ResponseEntity<TicketResponse> marcarIncompleto(@AuthenticatedSession CuentaAcceso cuenta, @PathVariable Long id,
                                                             @RequestBody MarcarIncompletoRequest request) {
        Operador operador = operadorDe(cuenta);
        Ticket ticket = ticketService.marcarIncompleto(id, operador, request.getObservacion());
        return ResponseEntity.ok(enriquecer(ticket));
    }

    @GetMapping("/asignados-a-mi")
    public List<TicketResponse> asignadosAMi(@AuthenticatedSession CuentaAcceso cuenta) {
        Operador operador = operadorDe(cuenta);
        return ticketService.listarAsignadosA(operador.getId()).stream().map(TicketResponse::from).toList();
    }

    // CU13
    @GetMapping("/sin-asignar")
    public List<TicketResponse> sinAsignar(@AuthenticatedSession CuentaAcceso cuenta) {
        autorizador.requireRol(cuenta, Rol.OPERADOR, Rol.ADMINISTRADOR);
        return ticketService.listarSinAsignar().stream().map(TicketResponse::from).toList();
    }

    @PostMapping("/{id}/asignar")
    public ResponseEntity<TicketResponse> asignar(@AuthenticatedSession CuentaAcceso cuenta, @PathVariable Long id,
                                                    @RequestBody AsignarOperadorRequest request) {
        Operador responsable = operadorDe(cuenta);
        Ticket ticket = ticketService.asignarOperador(id, request.getIdOperador(), responsable);
        return ResponseEntity.ok(enriquecer(ticket));
    }

    // CU14
    @PostMapping("/{id}/reasignar")
    public ResponseEntity<TicketResponse> reasignar(@AuthenticatedSession CuentaAcceso cuenta, @PathVariable Long id,
                                                      @RequestBody ReasignarOperadorRequest request) {
        Operador responsable = operadorDe(cuenta);
        Ticket ticket = ticketService.reasignarOperador(id, request.getIdOperadorNuevo(), request.getMotivo(), responsable);
        return ResponseEntity.ok(enriquecer(ticket));
    }

    // Transición operativa hacia RESUELTO
    @PostMapping("/{id}/resolver")
    public ResponseEntity<TicketResponse> resolver(@AuthenticatedSession CuentaAcceso cuenta, @PathVariable Long id) {
        Operador operador = operadorDe(cuenta);
        Ticket ticket = ticketService.marcarResuelto(id, operador);
        return ResponseEntity.ok(enriquecer(ticket));
    }

    // CU17
    @PostMapping("/{id}/cerrar")
    public ResponseEntity<TicketResponse> cerrar(@AuthenticatedSession CuentaAcceso cuenta, @PathVariable Long id,
                                                   @RequestBody CerrarTicketRequest request) {
        Operador operador = operadorDe(cuenta);
        Ticket ticket = ticketService.cerrarTicket(id, operador, request.getSolucion());
        return ResponseEntity.ok(enriquecer(ticket));
    }

    // CU18
    @PostMapping("/{id}/validar-resolucion")
    public ResponseEntity<TicketResponse> validarResolucion(@AuthenticatedSession CuentaAcceso cuenta, @PathVariable Long id) {
        UsuarioTicket usuario = usuarioDe(cuenta);
        Ticket ticket = ticketService.validarResolucion(id, usuario);
        return ResponseEntity.ok(enriquecer(ticket));
    }

    // CU19
    @PostMapping("/{id}/reabrir")
    public ResponseEntity<TicketResponse> reabrir(@AuthenticatedSession CuentaAcceso cuenta, @PathVariable Long id,
                                                    @RequestBody ReabrirTicketRequest request) {
        autorizador.requireRol(cuenta, Rol.USUARIO, Rol.OPERADOR);
        Ticket ticket = cuenta.getRol() == Rol.USUARIO
                ? ticketService.reabrirTicketPorUsuario(id, cuenta.getUsuarioTicket(), request.getMotivo())
                : ticketService.reabrirTicketPorOperador(id, cuenta.getOperador(), request.getMotivo());
        return ResponseEntity.ok(enriquecer(ticket));
    }

    private TicketResponse enriquecer(Ticket ticket) {
        TicketResponse response = TicketResponse.from(ticket);
        response.setAdjuntos(ticketService.listarAdjuntos(ticket.getId()).stream().map(AdjuntoResponse::from).toList());
        response.setHistorialEstados(ticket.getHistorialEstados().stream().map(HistorialEstadoResponse::from).toList());
        response.setHistorialAsignaciones(ticketService.listarHistorialAsignaciones(ticket.getId()).stream()
                .map(HistorialAsignacionResponse::from).toList());
        return response;
    }

    private UsuarioTicket usuarioDe(CuentaAcceso cuenta) {
        autorizador.requireRol(cuenta, Rol.USUARIO);
        return cuenta.getUsuarioTicket();
    }

    private Operador operadorDe(CuentaAcceso cuenta) {
        autorizador.requireRol(cuenta, Rol.OPERADOR, Rol.ADMINISTRADOR);
        return cuenta.getOperador();
    }

    private byte[] leerContenido(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el archivo adjunto", e);
        }
    }
}

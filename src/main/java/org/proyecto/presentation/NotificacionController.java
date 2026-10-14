package org.proyecto.presentation;

import org.proyecto.business.NotificacionService;
import org.proyecto.business.SesionActiva;
import org.proyecto.config.AuthenticatedSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Panel de notificaciones del destinatario (CU16 postcondición 2). Cada cuenta ve solo las suyas,
 * por eso no exige un permiso: alcanza con estar autenticado.
 */
@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @GetMapping
    public List<NotificacionResponse> mias(@AuthenticatedSession SesionActiva sesion) {
        return notificacionService.listarDe(sesion.cuenta()).stream().map(NotificacionResponse::from).toList();
    }

    @GetMapping("/no-leidas")
    public Map<String, Long> noLeidas(@AuthenticatedSession SesionActiva sesion) {
        return Map.of("cantidad", notificacionService.contarNoLeidas(sesion.cuenta()));
    }

    @PostMapping("/{id}/leida")
    public NotificacionResponse marcarLeida(@AuthenticatedSession SesionActiva sesion, @PathVariable Long id) {
        return NotificacionResponse.from(notificacionService.marcarLeida(id, sesion.cuenta()));
    }

    @PostMapping("/leidas")
    public ResponseEntity<Void> marcarTodasLeidas(@AuthenticatedSession SesionActiva sesion) {
        notificacionService.marcarTodasLeidas(sesion.cuenta());
        return ResponseEntity.noContent().build();
    }
}

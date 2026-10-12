package org.proyecto.presentation;

import org.proyecto.business.SesionActiva;
import org.proyecto.business.UsuarioService;
import org.proyecto.config.AuthenticatedSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Operaciones de cualquier usuario autenticado sobre su propia cuenta. */
@RestController
@RequestMapping("/api/cuenta")
public class CuentaController {

    private final UsuarioService usuarioService;

    public CuentaController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // Único endpoint habilitado con contraseña temporal: es la salida de ese estado.
    @PostMapping("/cambiar-password")
    public ResponseEntity<Void> cambiarPassword(@AuthenticatedSession(permitePasswordTemporal = true) SesionActiva sesion,
                                                @RequestBody CambiarPasswordRequest request) {
        usuarioService.cambiarPasswordPropia(sesion.cuenta().getId(), request.getPasswordActual(), request.getPasswordNueva());
        return ResponseEntity.noContent().build();
    }
}

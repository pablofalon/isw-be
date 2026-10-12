package org.proyecto.presentation;

import org.proyecto.business.Autorizador;
import org.proyecto.business.Permisos;
import org.proyecto.business.SesionActiva;
import org.proyecto.business.UsuarioService;
import org.proyecto.config.AuthenticatedSession;
import org.proyecto.entities.Rol;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** CU24 - ABM de usuarios. Todas las operaciones requieren USUARIOS_ADMINISTRAR. */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final Autorizador autorizador;

    public UsuarioController(UsuarioService usuarioService, Autorizador autorizador) {
        this.usuarioService = usuarioService;
        this.autorizador = autorizador;
    }

    @GetMapping
    public List<UsuarioResponse> listar(@AuthenticatedSession SesionActiva sesion,
                                        @RequestParam(required = false) Rol tipo,
                                        @RequestParam(required = false) Boolean activa) {
        autorizador.requirePermiso(sesion, Permisos.USUARIOS_ADMINISTRAR);
        return usuarioService.listar(tipo, activa).stream().map(UsuarioResponse::from).toList();
    }

    @GetMapping("/{id}")
    public UsuarioResponse obtener(@AuthenticatedSession SesionActiva sesion, @PathVariable Long id) {
        autorizador.requirePermiso(sesion, Permisos.USUARIOS_ADMINISTRAR);
        return UsuarioResponse.from(usuarioService.obtener(id));
    }

    @PostMapping
    public UsuarioResponse crear(@AuthenticatedSession SesionActiva sesion, @RequestBody AltaUsuarioRequest request) {
        autorizador.requirePermiso(sesion, Permisos.USUARIOS_ADMINISTRAR);
        return UsuarioResponse.from(usuarioService.crear(request.getTipo(), request.getUsuarioLogin(),
                request.getPasswordTemporal(), request.toDatosPersona()));
    }

    @PutMapping("/{id}")
    public UsuarioResponse modificar(@AuthenticatedSession SesionActiva sesion, @PathVariable Long id,
                                     @RequestBody ModificarUsuarioRequest request) {
        autorizador.requirePermiso(sesion, Permisos.USUARIOS_ADMINISTRAR);
        return UsuarioResponse.from(usuarioService.modificar(id, request.toDatosPersona()));
    }

    @PostMapping("/{id}/desactivar")
    public UsuarioResponse desactivar(@AuthenticatedSession SesionActiva sesion, @PathVariable Long id) {
        autorizador.requirePermiso(sesion, Permisos.USUARIOS_ADMINISTRAR);
        return UsuarioResponse.from(usuarioService.desactivar(id, sesion.cuenta().getId()));
    }

    @PostMapping("/{id}/activar")
    public UsuarioResponse activar(@AuthenticatedSession SesionActiva sesion, @PathVariable Long id) {
        autorizador.requirePermiso(sesion, Permisos.USUARIOS_ADMINISTRAR);
        return UsuarioResponse.from(usuarioService.activar(id));
    }

    @PostMapping("/{id}/blanquear-password")
    public UsuarioResponse blanquearPassword(@AuthenticatedSession SesionActiva sesion, @PathVariable Long id,
                                             @RequestBody BlanquearPasswordRequest request) {
        autorizador.requirePermiso(sesion, Permisos.USUARIOS_ADMINISTRAR);
        return UsuarioResponse.from(usuarioService.blanquearPassword(id, request.getPasswordTemporal()));
    }
}

package org.proyecto.presentation;

import org.proyecto.business.Autorizador;
import org.proyecto.business.Permisos;
import org.proyecto.business.PermisoService;
import org.proyecto.business.SesionActiva;
import org.proyecto.config.AuthenticatedSession;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

/** ABM del módulo de seguridad: árbol de grupos/permisos y su asignación a cuentas. */
@RestController
@RequestMapping("/api/seguridad")
public class SeguridadController {

    private final PermisoService permisoService;
    private final Autorizador autorizador;

    public SeguridadController(PermisoService permisoService, Autorizador autorizador) {
        this.permisoService = permisoService;
        this.autorizador = autorizador;
    }

    @GetMapping("/componentes")
    public List<ComponentePermisoResponse> listarComponentes(@AuthenticatedSession SesionActiva sesion) {
        autorizador.requirePermiso(sesion, Permisos.SEGURIDAD_ADMINISTRAR);
        return permisoService.listarComponentes().stream()
                .map(ComponentePermisoResponse::from)
                .sorted(Comparator.comparing(ComponentePermisoResponse::getId))
                .toList();
    }

    @PostMapping("/grupos")
    public ComponentePermisoResponse crearGrupo(@AuthenticatedSession SesionActiva sesion,
                                                  @RequestBody CrearGrupoRequest request) {
        autorizador.requirePermiso(sesion, Permisos.SEGURIDAD_ADMINISTRAR);
        return ComponentePermisoResponse.from(permisoService.crearGrupo(request.getCodigo(), request.getDescripcion()));
    }

    @PostMapping("/grupos/{id}/hijos")
    public ComponentePermisoResponse agregarHijo(@AuthenticatedSession SesionActiva sesion, @PathVariable Long id,
                                                 @RequestBody AgregarHijoRequest request) {
        autorizador.requirePermiso(sesion, Permisos.SEGURIDAD_ADMINISTRAR);
        return ComponentePermisoResponse.from(permisoService.agregarHijo(id, request.getIdComponente()));
    }

    @DeleteMapping("/grupos/{id}/hijos/{idHijo}")
    public ComponentePermisoResponse quitarHijo(@AuthenticatedSession SesionActiva sesion, @PathVariable Long id,
                                                @PathVariable Long idHijo) {
        autorizador.requirePermiso(sesion, Permisos.SEGURIDAD_ADMINISTRAR);
        return ComponentePermisoResponse.from(permisoService.quitarHijo(id, idHijo));
    }

    @GetMapping("/cuentas")
    public List<CuentaSeguridadResponse> listarCuentas(@AuthenticatedSession SesionActiva sesion) {
        autorizador.requirePermiso(sesion, Permisos.SEGURIDAD_ADMINISTRAR);
        return permisoService.listarCuentas().stream()
                .map(CuentaSeguridadResponse::from)
                .sorted(Comparator.comparing(CuentaSeguridadResponse::getId))
                .toList();
    }

    @PutMapping("/cuentas/{id}/componentes")
    public CuentaSeguridadResponse asignarComponentes(@AuthenticatedSession SesionActiva sesion, @PathVariable Long id,
                                                      @RequestBody AsignarComponentesRequest request) {
        autorizador.requirePermiso(sesion, Permisos.SEGURIDAD_ADMINISTRAR);
        return CuentaSeguridadResponse.from(permisoService.asignarComponentes(id, request.getIdsComponentes()));
    }
}

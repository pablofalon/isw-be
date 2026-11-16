package org.proyecto.presentation;

import org.proyecto.business.Autorizador;
import org.proyecto.business.GrupoParametros;
import org.proyecto.business.ParametroService;
import org.proyecto.business.Permisos;
import org.proyecto.business.SesionActiva;
import org.proyecto.config.AuthenticatedSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** CU25 - Configurar parámetros. Todas las operaciones requieren PARAMETROS_ADMINISTRAR. */
@RestController
@RequestMapping("/api/parametros")
public class ParametroController {

    private final ParametroService parametroService;
    private final Autorizador autorizador;

    public ParametroController(ParametroService parametroService, Autorizador autorizador) {
        this.parametroService = parametroService;
        this.autorizador = autorizador;
    }

    @GetMapping
    public ParametrosResponse obtener(@AuthenticatedSession SesionActiva sesion) {
        autorizador.requirePermiso(sesion, Permisos.PARAMETROS_ADMINISTRAR);
        return new ParametrosResponse(
                parametroService.listarCategorias().stream().map(ParametrosResponse.CategoriaAdminResponse::from).toList(),
                parametroService.listarPrioridades().stream().map(PrioridadResponse::from).toList(),
                parametroService.listarGenerales().stream().map(ParametrosResponse.ParametroGeneralResponse::from).toList());
    }

    @GetMapping("/auditoria")
    public List<ParametrosResponse.AuditoriaResponse> auditoria(@AuthenticatedSession SesionActiva sesion) {
        autorizador.requirePermiso(sesion, Permisos.PARAMETROS_ADMINISTRAR);
        return parametroService.ultimosCambios().stream().map(ParametrosResponse.AuditoriaResponse::from).toList();
    }

    @PostMapping("/categorias")
    public ParametrosResponse.CategoriaAdminResponse crearCategoria(@AuthenticatedSession SesionActiva sesion,
                                                                    @RequestBody CategoriaRequest request) {
        autorizador.requirePermiso(sesion, Permisos.PARAMETROS_ADMINISTRAR);
        return ParametrosResponse.CategoriaAdminResponse.from(parametroService.crearCategoria(
                administrador(sesion), request.nombre(), request.descripcion(), request.area()));
    }

    @PutMapping("/categorias/{id}")
    public ParametrosResponse.CategoriaAdminResponse modificarCategoria(@AuthenticatedSession SesionActiva sesion,
                                                                        @PathVariable Long id,
                                                                        @RequestBody CategoriaRequest request) {
        autorizador.requirePermiso(sesion, Permisos.PARAMETROS_ADMINISTRAR);
        return ParametrosResponse.CategoriaAdminResponse.from(parametroService.modificarCategoria(
                administrador(sesion), id, request.nombre(), request.descripcion(), request.area()));
    }

    @PostMapping("/categorias/{id}/desactivar")
    public ParametrosResponse.CategoriaAdminResponse desactivarCategoria(@AuthenticatedSession SesionActiva sesion,
                                                                         @PathVariable Long id) {
        autorizador.requirePermiso(sesion, Permisos.PARAMETROS_ADMINISTRAR);
        return ParametrosResponse.CategoriaAdminResponse.from(
                parametroService.cambiarEstadoCategoria(administrador(sesion), id, false));
    }

    @PostMapping("/categorias/{id}/activar")
    public ParametrosResponse.CategoriaAdminResponse activarCategoria(@AuthenticatedSession SesionActiva sesion,
                                                                      @PathVariable Long id) {
        autorizador.requirePermiso(sesion, Permisos.PARAMETROS_ADMINISTRAR);
        return ParametrosResponse.CategoriaAdminResponse.from(
                parametroService.cambiarEstadoCategoria(administrador(sesion), id, true));
    }

    @PutMapping("/prioridades/{id}")
    public PrioridadResponse modificarPrioridad(@AuthenticatedSession SesionActiva sesion, @PathVariable Long id,
                                                @RequestBody ModificarPrioridadRequest request) {
        autorizador.requirePermiso(sesion, Permisos.PARAMETROS_ADMINISTRAR);
        return PrioridadResponse.from(parametroService.modificarPrioridad(
                administrador(sesion), id, request.descripcion(), request.tiempoObjetivoRespuesta()));
    }

    @PutMapping("/generales/{clave}")
    public ParametrosResponse.ParametroGeneralResponse modificarGeneral(@AuthenticatedSession SesionActiva sesion,
                                                                        @PathVariable String clave,
                                                                        @RequestBody ModificarParametroRequest request) {
        autorizador.requirePermiso(sesion, Permisos.PARAMETROS_ADMINISTRAR);
        return ParametrosResponse.ParametroGeneralResponse.from(
                parametroService.modificarGeneral(administrador(sesion), clave, request.valor()));
    }

    @PostMapping("/{grupo}/restaurar")
    public ParametrosResponse restaurar(@AuthenticatedSession SesionActiva sesion, @PathVariable GrupoParametros grupo) {
        autorizador.requirePermiso(sesion, Permisos.PARAMETROS_ADMINISTRAR);
        parametroService.restaurar(administrador(sesion), grupo);
        return obtener(sesion);
    }

    private static String administrador(SesionActiva sesion) {
        return sesion.cuenta().getUsuarioLogin();
    }
}

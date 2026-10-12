package org.proyecto.business;

import org.proyecto.data.ComponentePermisoRepository;
import org.proyecto.data.CuentaAccesoRepository;
import org.proyecto.entities.ComponentePermiso;
import org.proyecto.entities.CuentaAcceso;
import org.proyecto.entities.GrupoPermisos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Cliente del Composite: resuelve los permisos efectivos de una cuenta y administra el árbol de
 * grupos y su asignación a cuentas. Los permisos son los que define el código (ver Permisos),
 * por eso el ABM solo crea grupos y los arma con componentes existentes.
 */
@Service
public class PermisoService {

    private final ComponentePermisoRepository componenteRepository;
    private final CuentaAccesoRepository cuentaAccesoRepository;

    public PermisoService(ComponentePermisoRepository componenteRepository,
                          CuentaAccesoRepository cuentaAccesoRepository) {
        this.componenteRepository = componenteRepository;
        this.cuentaAccesoRepository = cuentaAccesoRepository;
    }

    @Transactional(readOnly = true)
    public Set<String> permisosDe(Long cuentaId) {
        return cuentaAccesoRepository.findById(cuentaId)
                .map(CuentaAcceso::obtenerPermisos)
                .orElse(Set.of());
    }

    public boolean tienePermiso(CuentaAcceso cuenta, String permiso) {
        return cuenta.obtenerPermisos().contains(permiso);
    }

    @Transactional(readOnly = true)
    public List<ComponentePermiso> listarComponentes() {
        return componenteRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<CuentaAcceso> listarCuentas() {
        return cuentaAccesoRepository.findAll();
    }

    @Transactional
    public GrupoPermisos crearGrupo(String codigo, String descripcion) {
        if (codigo == null || codigo.isBlank()) {
            throw new OperacionSeguridadInvalidaException("El código del grupo es obligatorio");
        }
        String codigoNormalizado = codigo.trim().toUpperCase().replace(' ', '_');
        if (componenteRepository.existsByCodigo(codigoNormalizado)) {
            throw new OperacionSeguridadInvalidaException("Ya existe un componente con el código " + codigoNormalizado);
        }
        return componenteRepository.save(new GrupoPermisos(codigoNormalizado, descripcion));
    }

    @Transactional
    public GrupoPermisos agregarHijo(Long grupoId, Long componenteId) {
        GrupoPermisos grupo = buscarGrupo(grupoId);
        grupo.agregar(buscarComponente(componenteId));
        recalcularSesiones();
        return grupo;
    }

    @Transactional
    public GrupoPermisos quitarHijo(Long grupoId, Long componenteId) {
        GrupoPermisos grupo = buscarGrupo(grupoId);
        grupo.quitar(buscarComponente(componenteId));
        validarQueQuedeUnAdministrador();
        recalcularSesiones();
        return grupo;
    }

    @Transactional
    public CuentaAcceso asignarComponentes(Long cuentaId, List<Long> idsComponentes) {
        CuentaAcceso cuenta = cuentaAccesoRepository.findById(cuentaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la cuenta " + cuentaId));
        List<ComponentePermiso> componentes = new ArrayList<>();
        for (Long id : new LinkedHashSet<>(idsComponentes == null ? List.of() : idsComponentes)) {
            componentes.add(buscarComponente(id));
        }
        cuenta.getComponentes().clear();
        cuenta.getComponentes().addAll(componentes);
        validarQueQuedeUnAdministrador();
        recalcularSesiones();
        return cuenta;
    }

    /**
     * Evita que un cambio deje al sistema sin nadie capaz de administrar la seguridad (por ejemplo,
     * el admin quitándose a sí mismo el grupo ADMINISTRADOR). Al lanzar, la transacción se revierte.
     */
    public void validarQueQuedeUnAdministrador() {
        boolean hayAdministrador = cuentaAccesoRepository.findAll().stream()
                .anyMatch(cuenta -> cuenta.isActiva() && tienePermiso(cuenta, Permisos.SEGURIDAD_ADMINISTRAR));
        if (!hayAdministrador) {
            throw new OperacionSeguridadInvalidaException(
                    "El cambio dejaría al sistema sin ninguna cuenta activa con " + Permisos.SEGURIDAD_ADMINISTRAR);
        }
    }

    private void recalcularSesiones() {
        SessionManager.getInstance().recalcularPermisos(this::permisosDe);
    }

    private GrupoPermisos buscarGrupo(Long id) {
        ComponentePermiso componente = buscarComponente(id);
        if (!(componente instanceof GrupoPermisos grupo)) {
            throw new OperacionSeguridadInvalidaException(componente.getCodigo() + " es un permiso, no un grupo");
        }
        return grupo;
    }

    private ComponentePermiso buscarComponente(Long id) {
        return componenteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el componente de permiso " + id));
    }
}

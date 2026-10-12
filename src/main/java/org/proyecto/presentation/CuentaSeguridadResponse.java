package org.proyecto.presentation;

import org.proyecto.entities.ComponentePermiso;
import org.proyecto.entities.CuentaAcceso;
import org.proyecto.entities.Rol;

import java.util.List;

/** Cuenta vista desde el ABM de seguridad: componentes asignados directamente y permisos efectivos resultantes. */
public class CuentaSeguridadResponse {

    private Long id;
    private String usuarioLogin;
    private Rol rol;
    private boolean activa;
    private List<Long> componentes;
    private List<String> permisos;

    public CuentaSeguridadResponse() {
    }

    public static CuentaSeguridadResponse from(CuentaAcceso cuenta) {
        CuentaSeguridadResponse response = new CuentaSeguridadResponse();
        response.id = cuenta.getId();
        response.usuarioLogin = cuenta.getUsuarioLogin();
        response.rol = cuenta.getRol();
        response.activa = cuenta.isActiva();
        response.componentes = cuenta.getComponentes().stream().map(ComponentePermiso::getId).toList();
        response.permisos = cuenta.obtenerPermisos().stream().sorted().toList();
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsuarioLogin() {
        return usuarioLogin;
    }

    public void setUsuarioLogin(String usuarioLogin) {
        this.usuarioLogin = usuarioLogin;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public boolean isActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }

    public List<Long> getComponentes() {
        return componentes;
    }

    public void setComponentes(List<Long> componentes) {
        this.componentes = componentes;
    }

    public List<String> getPermisos() {
        return permisos;
    }

    public void setPermisos(List<String> permisos) {
        this.permisos = permisos;
    }
}

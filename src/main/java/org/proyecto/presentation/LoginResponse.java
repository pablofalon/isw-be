package org.proyecto.presentation;

import org.proyecto.entities.CuentaAcceso;
import org.proyecto.entities.Rol;

import java.util.Collection;
import java.util.List;

public class LoginResponse {

    private String token;
    private Rol rol;
    private String nombreCompleto;
    private List<String> permisos;
    private boolean debeCambiarPassword;

    public LoginResponse() {
    }

    public static LoginResponse from(String token, CuentaAcceso cuenta, Collection<String> permisos) {
        LoginResponse response = new LoginResponse();
        response.token = token;
        response.rol = cuenta.getRol();
        response.permisos = permisos.stream().sorted().toList();
        response.debeCambiarPassword = cuenta.isDebeCambiarPassword();
        if (cuenta.getUsuarioTicket() != null) {
            response.nombreCompleto = cuenta.getUsuarioTicket().getNombre() + " " + cuenta.getUsuarioTicket().getApellido();
        } else if (cuenta.getOperador() != null) {
            response.nombreCompleto = cuenta.getOperador().getNombre() + " " + cuenta.getOperador().getApellido();
        }
        return response;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public List<String> getPermisos() {
        return permisos;
    }

    public void setPermisos(List<String> permisos) {
        this.permisos = permisos;
    }

    public boolean isDebeCambiarPassword() {
        return debeCambiarPassword;
    }

    public void setDebeCambiarPassword(boolean debeCambiarPassword) {
        this.debeCambiarPassword = debeCambiarPassword;
    }
}

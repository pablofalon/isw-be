package org.proyecto.presentation;

import org.proyecto.entities.CuentaAcceso;
import org.proyecto.entities.Rol;

public class LoginResponse {

    private String token;
    private Rol rol;
    private String nombreCompleto;

    public LoginResponse() {
    }

    public static LoginResponse from(String token, CuentaAcceso cuenta) {
        LoginResponse response = new LoginResponse();
        response.token = token;
        response.rol = cuenta.getRol();
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
}

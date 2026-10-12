package org.proyecto.presentation;

import org.proyecto.business.DatosPersona;
import org.proyecto.entities.Rol;

public class AltaUsuarioRequest {

    private Rol tipo;
    private String usuarioLogin;
    private String passwordTemporal;
    private String nombre;
    private String apellido;
    private String correoElectronico;
    private String telefono;
    private String area;
    private String cargo;

    public AltaUsuarioRequest() {
    }

    public Rol getTipo() {
        return tipo;
    }

    public void setTipo(Rol tipo) {
        this.tipo = tipo;
    }

    public String getUsuarioLogin() {
        return usuarioLogin;
    }

    public void setUsuarioLogin(String usuarioLogin) {
        this.usuarioLogin = usuarioLogin;
    }

    public String getPasswordTemporal() {
        return passwordTemporal;
    }

    public void setPasswordTemporal(String passwordTemporal) {
        this.passwordTemporal = passwordTemporal;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getCorreoElectronico() {
        return correoElectronico;
    }

    public void setCorreoElectronico(String correoElectronico) {
        this.correoElectronico = correoElectronico;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }

    public DatosPersona toDatosPersona() {
        return new DatosPersona(nombre, apellido, correoElectronico, telefono, area, cargo);
    }
}

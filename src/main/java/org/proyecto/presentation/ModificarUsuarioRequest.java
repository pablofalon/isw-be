package org.proyecto.presentation;

import org.proyecto.business.DatosPersona;

public class ModificarUsuarioRequest {

    private String nombre;
    private String apellido;
    private String correoElectronico;
    private String telefono;
    private String area;
    private String cargo;

    public ModificarUsuarioRequest() {
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

package org.proyecto.presentation;

import org.proyecto.entities.UsuarioTicket;

public class UsuarioResumen {

    private Long id;
    private String nombre;
    private String apellido;
    private String correoElectronico;

    public UsuarioResumen() {
    }

    public static UsuarioResumen from(UsuarioTicket usuario) {
        if (usuario == null) {
            return null;
        }
        UsuarioResumen resumen = new UsuarioResumen();
        resumen.id = usuario.getId();
        resumen.nombre = usuario.getNombre();
        resumen.apellido = usuario.getApellido();
        resumen.correoElectronico = usuario.getCorreoElectronico();
        return resumen;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
}

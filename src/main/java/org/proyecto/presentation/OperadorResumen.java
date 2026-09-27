package org.proyecto.presentation;

import org.proyecto.entities.Operador;

public class OperadorResumen {

    private Long id;
    private String nombre;
    private String apellido;
    private String correoElectronico;
    private String area;
    private boolean activo;

    public OperadorResumen() {
    }

    public static OperadorResumen from(Operador operador) {
        if (operador == null) {
            return null;
        }
        OperadorResumen resumen = new OperadorResumen();
        resumen.id = operador.getId();
        resumen.nombre = operador.getNombre();
        resumen.apellido = operador.getApellido();
        resumen.correoElectronico = operador.getCorreoElectronico();
        resumen.area = operador.getArea();
        resumen.activo = operador.isActivo();
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

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}

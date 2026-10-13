package org.proyecto.presentation;

import org.proyecto.entities.Categoria;

public class CategoriaResponse {

    private Long id;
    private String nombre;
    private String descripcion;
    private String area;

    public CategoriaResponse() {
    }

    public static CategoriaResponse from(Categoria categoria) {
        if (categoria == null) {
            return null;
        }
        CategoriaResponse response = new CategoriaResponse();
        response.id = categoria.getId();
        response.nombre = categoria.getNombre();
        response.descripcion = categoria.getDescripcion();
        response.area = categoria.getArea();
        return response;
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

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }
}

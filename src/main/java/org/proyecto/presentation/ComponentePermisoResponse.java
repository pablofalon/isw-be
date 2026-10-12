package org.proyecto.presentation;

import org.proyecto.entities.ComponentePermiso;

import java.util.List;

/** Nodo del árbol de permisos; los hijos van por id para que el cliente arme el árbol sin repetir nodos compartidos. */
public class ComponentePermisoResponse {

    private Long id;
    private String codigo;
    private String descripcion;
    private String tipo;
    private List<Long> hijos;

    public ComponentePermisoResponse() {
    }

    public static ComponentePermisoResponse from(ComponentePermiso componente) {
        ComponentePermisoResponse response = new ComponentePermisoResponse();
        response.id = componente.getId();
        response.codigo = componente.getCodigo();
        response.descripcion = componente.getDescripcion();
        response.tipo = componente.getTipo();
        response.hijos = componente.getHijos().stream().map(ComponentePermiso::getId).toList();
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public List<Long> getHijos() {
        return hijos;
    }

    public void setHijos(List<Long> hijos) {
        this.hijos = hijos;
    }
}

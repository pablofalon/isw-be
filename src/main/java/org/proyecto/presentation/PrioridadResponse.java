package org.proyecto.presentation;

import org.proyecto.entities.NivelPrioridad;
import org.proyecto.entities.Prioridad;

public class PrioridadResponse {

    private Long id;
    private NivelPrioridad nivel;
    private String descripcion;
    private Integer tiempoObjetivoRespuesta;

    public PrioridadResponse() {
    }

    public static PrioridadResponse from(Prioridad prioridad) {
        if (prioridad == null) {
            return null;
        }
        PrioridadResponse response = new PrioridadResponse();
        response.id = prioridad.getId();
        response.nivel = prioridad.getNivel();
        response.descripcion = prioridad.getDescripcion();
        response.tiempoObjetivoRespuesta = prioridad.getTiempoObjetivoRespuesta();
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public NivelPrioridad getNivel() {
        return nivel;
    }

    public void setNivel(NivelPrioridad nivel) {
        this.nivel = nivel;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Integer getTiempoObjetivoRespuesta() {
        return tiempoObjetivoRespuesta;
    }

    public void setTiempoObjetivoRespuesta(Integer tiempoObjetivoRespuesta) {
        this.tiempoObjetivoRespuesta = tiempoObjetivoRespuesta;
    }
}

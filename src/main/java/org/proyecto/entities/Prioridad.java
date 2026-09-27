package org.proyecto.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "prioridades")
public class Prioridad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private NivelPrioridad nivel;

    private String descripcion;

    /** Tiempo objetivo de respuesta, en horas. */
    private Integer tiempoObjetivoRespuesta;

    public Prioridad() {
    }

    public Prioridad(NivelPrioridad nivel, String descripcion, Integer tiempoObjetivoRespuesta) {
        this.nivel = nivel;
        this.descripcion = descripcion;
        this.tiempoObjetivoRespuesta = tiempoObjetivoRespuesta;
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

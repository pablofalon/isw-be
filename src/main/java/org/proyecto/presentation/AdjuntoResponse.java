package org.proyecto.presentation;

import org.proyecto.entities.Adjunto;

import java.time.LocalDateTime;

public class AdjuntoResponse {

    private Long id;
    private String nombreArchivo;
    private String tipoArchivo;
    private long tamano;
    private LocalDateTime fechaCarga;
    private String descripcion;

    public AdjuntoResponse() {
    }

    public static AdjuntoResponse from(Adjunto adjunto) {
        AdjuntoResponse response = new AdjuntoResponse();
        response.id = adjunto.getId();
        response.nombreArchivo = adjunto.getNombreArchivo();
        response.tipoArchivo = adjunto.getTipoArchivo();
        response.tamano = adjunto.getTamano();
        response.fechaCarga = adjunto.getFechaCarga();
        response.descripcion = adjunto.getDescripcion();
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    public String getTipoArchivo() {
        return tipoArchivo;
    }

    public void setTipoArchivo(String tipoArchivo) {
        this.tipoArchivo = tipoArchivo;
    }

    public long getTamano() {
        return tamano;
    }

    public void setTamano(long tamano) {
        this.tamano = tamano;
    }

    public LocalDateTime getFechaCarga() {
        return fechaCarga;
    }

    public void setFechaCarga(LocalDateTime fechaCarga) {
        this.fechaCarga = fechaCarga;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}

package org.proyecto.presentation;

import org.proyecto.entities.HistorialEstado;

import java.time.LocalDateTime;

public class HistorialEstadoResponse {

    private String estado;
    private String observacion;
    private String motivoCambio;
    private LocalDateTime fechaCambio;
    private UsuarioResumen actorUsuario;
    private OperadorResumen actorOperador;

    public HistorialEstadoResponse() {
    }

    public static HistorialEstadoResponse from(HistorialEstado historial) {
        HistorialEstadoResponse response = new HistorialEstadoResponse();
        response.estado = historial.getTicketEstado().getNombre();
        response.observacion = historial.getObservacion();
        response.motivoCambio = historial.getMotivoCambio();
        response.fechaCambio = historial.getFechaCambio();
        response.actorUsuario = UsuarioResumen.from(historial.getActorUsuario());
        response.actorOperador = OperadorResumen.from(historial.getActorOperador());
        return response;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public String getMotivoCambio() {
        return motivoCambio;
    }

    public void setMotivoCambio(String motivoCambio) {
        this.motivoCambio = motivoCambio;
    }

    public LocalDateTime getFechaCambio() {
        return fechaCambio;
    }

    public void setFechaCambio(LocalDateTime fechaCambio) {
        this.fechaCambio = fechaCambio;
    }

    public UsuarioResumen getActorUsuario() {
        return actorUsuario;
    }

    public void setActorUsuario(UsuarioResumen actorUsuario) {
        this.actorUsuario = actorUsuario;
    }

    public OperadorResumen getActorOperador() {
        return actorOperador;
    }

    public void setActorOperador(OperadorResumen actorOperador) {
        this.actorOperador = actorOperador;
    }
}

package org.proyecto.presentation;

import org.proyecto.entities.HistorialAsignacion;
import org.proyecto.entities.TipoAccionAsignacion;

import java.time.LocalDateTime;

public class HistorialAsignacionResponse {

    private TipoAccionAsignacion tipoAccion;
    private String motivo;
    private LocalDateTime fechaAsignacion;
    private OperadorResumen operadorAnterior;
    private OperadorResumen operadorNuevo;
    private OperadorResumen operadorResponsable;

    public HistorialAsignacionResponse() {
    }

    public static HistorialAsignacionResponse from(HistorialAsignacion historial) {
        HistorialAsignacionResponse response = new HistorialAsignacionResponse();
        response.tipoAccion = historial.getTipoAccion();
        response.motivo = historial.getMotivo();
        response.fechaAsignacion = historial.getFechaAsignacion();
        response.operadorAnterior = OperadorResumen.from(historial.getOperadorAnterior());
        response.operadorNuevo = OperadorResumen.from(historial.getOperadorNuevo());
        response.operadorResponsable = OperadorResumen.from(historial.getOperadorResponsable());
        return response;
    }

    public TipoAccionAsignacion getTipoAccion() {
        return tipoAccion;
    }

    public void setTipoAccion(TipoAccionAsignacion tipoAccion) {
        this.tipoAccion = tipoAccion;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public LocalDateTime getFechaAsignacion() {
        return fechaAsignacion;
    }

    public void setFechaAsignacion(LocalDateTime fechaAsignacion) {
        this.fechaAsignacion = fechaAsignacion;
    }

    public OperadorResumen getOperadorAnterior() {
        return operadorAnterior;
    }

    public void setOperadorAnterior(OperadorResumen operadorAnterior) {
        this.operadorAnterior = operadorAnterior;
    }

    public OperadorResumen getOperadorNuevo() {
        return operadorNuevo;
    }

    public void setOperadorNuevo(OperadorResumen operadorNuevo) {
        this.operadorNuevo = operadorNuevo;
    }

    public OperadorResumen getOperadorResponsable() {
        return operadorResponsable;
    }

    public void setOperadorResponsable(OperadorResumen operadorResponsable) {
        this.operadorResponsable = operadorResponsable;
    }
}

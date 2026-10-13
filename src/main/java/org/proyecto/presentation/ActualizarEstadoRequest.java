package org.proyecto.presentation;

import org.proyecto.entities.TicketEstadoCodigo;

public class ActualizarEstadoRequest {

    private TicketEstadoCodigo estado;
    private String observacion;

    public ActualizarEstadoRequest() {
    }

    public TicketEstadoCodigo getEstado() {
        return estado;
    }

    public void setEstado(TicketEstadoCodigo estado) {
        this.estado = estado;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }
}

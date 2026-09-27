package org.proyecto.presentation;

public class ReasignarOperadorRequest {

    private Long idOperadorNuevo;
    private String motivo;

    public ReasignarOperadorRequest() {
    }

    public Long getIdOperadorNuevo() {
        return idOperadorNuevo;
    }

    public void setIdOperadorNuevo(Long idOperadorNuevo) {
        this.idOperadorNuevo = idOperadorNuevo;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}

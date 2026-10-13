package org.proyecto.presentation;

import org.proyecto.business.LimitesAdjuntos;

import java.util.List;

/** Límites de CU02 para que el front valide antes de subir (FA1, FA2, E2). */
public class LimitesAdjuntosResponse {

    private long tamanioMaximoBytes;
    private int maximoPorTicket;
    private List<String> extensionesPermitidas;

    public LimitesAdjuntosResponse() {
    }

    public static LimitesAdjuntosResponse from(LimitesAdjuntos limites) {
        LimitesAdjuntosResponse response = new LimitesAdjuntosResponse();
        response.tamanioMaximoBytes = limites.getTamanioMaximoBytes();
        response.maximoPorTicket = limites.getMaximoPorTicket();
        response.extensionesPermitidas = LimitesAdjuntos.EXTENSIONES_PERMITIDAS;
        return response;
    }

    public long getTamanioMaximoBytes() {
        return tamanioMaximoBytes;
    }

    public void setTamanioMaximoBytes(long tamanioMaximoBytes) {
        this.tamanioMaximoBytes = tamanioMaximoBytes;
    }

    public int getMaximoPorTicket() {
        return maximoPorTicket;
    }

    public void setMaximoPorTicket(int maximoPorTicket) {
        this.maximoPorTicket = maximoPorTicket;
    }

    public List<String> getExtensionesPermitidas() {
        return extensionesPermitidas;
    }

    public void setExtensionesPermitidas(List<String> extensionesPermitidas) {
        this.extensionesPermitidas = extensionesPermitidas;
    }
}

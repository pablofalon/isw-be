package org.proyecto.presentation;

import java.util.List;

public class AsignarComponentesRequest {

    private List<Long> idsComponentes;

    public AsignarComponentesRequest() {
    }

    public List<Long> getIdsComponentes() {
        return idsComponentes;
    }

    public void setIdsComponentes(List<Long> idsComponentes) {
        this.idsComponentes = idsComponentes;
    }
}

package org.proyecto.business;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Límites configurables de CU02 (app.adjuntos.* en application.properties): tamaño máximo por
 * archivo (FA2) y cantidad máxima de adjuntos por ticket (E2).
 */
@Component
public class LimitesAdjuntos {

    public static final List<String> EXTENSIONES_PERMITIDAS = List.of("jpg", "jpeg", "png", "pdf", "docx", "xlsx");

    private final int tamanioMaximoMb;
    private final int maximoPorTicket;

    public LimitesAdjuntos(@Value("${app.adjuntos.tamanio-maximo-mb:5}") int tamanioMaximoMb,
                           @Value("${app.adjuntos.maximo-por-ticket:5}") int maximoPorTicket) {
        this.tamanioMaximoMb = tamanioMaximoMb;
        this.maximoPorTicket = maximoPorTicket;
    }

    public int getTamanioMaximoMb() {
        return tamanioMaximoMb;
    }

    public long getTamanioMaximoBytes() {
        return tamanioMaximoMb * 1024L * 1024L;
    }

    public int getMaximoPorTicket() {
        return maximoPorTicket;
    }
}

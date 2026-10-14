package org.proyecto.business;

import org.proyecto.entities.TicketEstadoCodigo;

import java.time.LocalDate;

/** Filtros opcionales del historial del ticket (CU12 paso 5). */
public record FiltroHistorial(TicketEstadoCodigo estado, LocalDate desde, LocalDate hasta) {

    public static FiltroHistorial sinFiltros() {
        return new FiltroHistorial(null, null, null);
    }
}

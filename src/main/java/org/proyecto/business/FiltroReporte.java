package org.proyecto.business;

import org.proyecto.entities.TicketEstadoCodigo;

/** Filtros opcionales de CU20 (paso 2); null = sin filtrar. */
public record FiltroReporte(Long idCategoria, Long idPrioridad, TicketEstadoCodigo estado, Long idOperador) {

    public static FiltroReporte sinFiltros() {
        return new FiltroReporte(null, null, null, null);
    }
}

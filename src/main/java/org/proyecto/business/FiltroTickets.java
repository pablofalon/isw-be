package org.proyecto.business;

import org.proyecto.entities.TicketEstadoCodigo;

import java.time.LocalDate;

/**
 * Filtros opcionales del listado operativo (CU10 paso 4). El estado permite incluir los finales
 * (FA2); numero acepta el número de ticket tal como se muestra (TK-000034) o solo los dígitos.
 */
public record FiltroTickets(TicketEstadoCodigo estado, Long idCategoria, Long idPrioridad, Long idOperador,
                            LocalDate desde, LocalDate hasta, String numero) {

    public static FiltroTickets sinFiltros() {
        return new FiltroTickets(null, null, null, null, null, null, null);
    }
}

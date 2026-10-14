package org.proyecto.business;

import java.time.LocalDate;

/** Filtros opcionales del monitoreo de tiempos de respuesta (CU11 paso 6). */
public record FiltroTiempos(Long idCategoria, Long idPrioridad, CumplimientoTiempo cumplimiento,
                            LocalDate desde, LocalDate hasta) {

    public static FiltroTiempos sinFiltros() {
        return new FiltroTiempos(null, null, null, null, null);
    }
}

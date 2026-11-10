package org.proyecto.business;

import java.time.LocalDate;
import java.util.List;

/** Indicadores de desempeño de CU21 sobre los tickets creados en el período; sin datos todo queda en cero. */
public record Indicadores(LocalDate desde, LocalDate hasta, long total, long abiertos, long cerrados,
                          double promedioResolucionHoras, List<Distribucion> porCategoria,
                          List<Distribucion> porPrioridad, List<Distribucion> recurrentes) {

    public record Distribucion(String clave, long cantidad) {
    }

    public boolean sinDatos() {
        return total == 0;
    }
}

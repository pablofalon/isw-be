package org.proyecto.business;

import java.time.LocalDate;
import java.util.List;

/**
 * Reporte de gestión de CU20. No se persiste: se calcula al consultar. El tiempo promedio de
 * resolución (en horas) se calcula sobre los tickets cerrados y es null cuando no hay ninguno.
 */
public record Reporte(LocalDate desde, LocalDate hasta, CriterioAgrupamiento criterio,
                      List<Grupo> grupos, Totales totales) {

    public record Grupo(String clave, long cantidad, long abiertos, long cerrados, Double promedioResolucionHoras) {
    }

    public record Totales(long cantidad, long abiertos, long cerrados, Double promedioResolucionHoras) {
    }

    /** FA2 de CU20: ningún ticket cumple los criterios. */
    public boolean sinDatos() {
        return totales.cantidad() == 0;
    }
}

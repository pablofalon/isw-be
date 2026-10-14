package org.proyecto.presentation;

import org.proyecto.business.CriterioAgrupamiento;
import org.proyecto.business.Reporte;

import java.time.LocalDate;
import java.util.List;

/** CU20. sinDatos permite a la interfaz mostrar el mensaje de FA2 sin recorrer los grupos. */
public record ReporteResponse(LocalDate desde, LocalDate hasta, CriterioAgrupamiento criterio,
                              List<GrupoResponse> grupos, TotalesResponse totales, boolean sinDatos) {

    public record GrupoResponse(String clave, long cantidad, long abiertos, long cerrados,
                                Double promedioResolucionHoras) {
    }

    public record TotalesResponse(long cantidad, long abiertos, long cerrados, Double promedioResolucionHoras) {
    }

    public static ReporteResponse from(Reporte reporte) {
        return new ReporteResponse(reporte.desde(), reporte.hasta(), reporte.criterio(),
                reporte.grupos().stream()
                        .map(g -> new GrupoResponse(g.clave(), g.cantidad(), g.abiertos(), g.cerrados(),
                                g.promedioResolucionHoras()))
                        .toList(),
                new TotalesResponse(reporte.totales().cantidad(), reporte.totales().abiertos(),
                        reporte.totales().cerrados(), reporte.totales().promedioResolucionHoras()),
                reporte.sinDatos());
    }
}

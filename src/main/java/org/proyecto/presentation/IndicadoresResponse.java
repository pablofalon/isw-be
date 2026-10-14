package org.proyecto.presentation;

import org.proyecto.business.Indicadores;

import java.time.LocalDate;
import java.util.List;

/** CU21. */
public record IndicadoresResponse(LocalDate desde, LocalDate hasta, long total, long abiertos, long cerrados,
                                  double promedioResolucionHoras, List<DistribucionResponse> porCategoria,
                                  List<DistribucionResponse> porPrioridad, List<DistribucionResponse> recurrentes,
                                  boolean sinDatos) {

    public record DistribucionResponse(String clave, long cantidad) {
    }

    public static IndicadoresResponse from(Indicadores indicadores) {
        return new IndicadoresResponse(indicadores.desde(), indicadores.hasta(), indicadores.total(),
                indicadores.abiertos(), indicadores.cerrados(), indicadores.promedioResolucionHoras(),
                convertir(indicadores.porCategoria()), convertir(indicadores.porPrioridad()),
                convertir(indicadores.recurrentes()), indicadores.sinDatos());
    }

    private static List<DistribucionResponse> convertir(List<Indicadores.Distribucion> distribuciones) {
        return distribuciones.stream().map(d -> new DistribucionResponse(d.clave(), d.cantidad())).toList();
    }
}

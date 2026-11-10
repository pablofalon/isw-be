package org.proyecto.business;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** CSV en UTF-8 con BOM (para que Excel respete los acentos); los números llevan punto decimal. */
@Component
public class CsvExportadorReporte implements ExportadorReporte {

    private static final String BOM = "﻿";
    private static final String SEPARADOR_DE_LINEA = "\r\n";

    @Override
    public FormatoExportacion formato() {
        return FormatoExportacion.CSV;
    }

    @Override
    public byte[] exportar(Reporte reporte, boolean incluirTotales) {
        StringBuilder csv = new StringBuilder(BOM);
        csv.append("Reporte de tickets").append(SEPARADOR_DE_LINEA);
        csv.append("Período,").append(reporte.desde()).append(" a ").append(reporte.hasta()).append(SEPARADOR_DE_LINEA);
        csv.append("Agrupado por,").append(reporte.criterio()).append(SEPARADOR_DE_LINEA);
        csv.append(SEPARADOR_DE_LINEA);
        csv.append(linea(reporte.criterio().name(), "Tickets", "Abiertos", "Cerrados", "Promedio de resolución (h)"));
        for (Reporte.Grupo grupo : reporte.grupos()) {
            csv.append(linea(grupo.clave(), String.valueOf(grupo.cantidad()), String.valueOf(grupo.abiertos()),
                    String.valueOf(grupo.cerrados()), horas(grupo.promedioResolucionHoras())));
        }
        if (incluirTotales) {
            Reporte.Totales totales = reporte.totales();
            csv.append(linea("Total", String.valueOf(totales.cantidad()), String.valueOf(totales.abiertos()),
                    String.valueOf(totales.cerrados()), horas(totales.promedioResolucionHoras())));
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String linea(String... celdas) {
        StringBuilder linea = new StringBuilder();
        for (int i = 0; i < celdas.length; i++) {
            if (i > 0) {
                linea.append(',');
            }
            linea.append(escapar(celdas[i]));
        }
        return linea.append(SEPARADOR_DE_LINEA).toString();
    }

    private String escapar(String valor) {
        if (valor.contains(",") || valor.contains("\"") || valor.contains("\n")) {
            return '"' + valor.replace("\"", "\"\"") + '"';
        }
        return valor;
    }

    private String horas(Double valor) {
        return valor == null ? "" : String.format(Locale.ROOT, "%.2f", valor);
    }
}

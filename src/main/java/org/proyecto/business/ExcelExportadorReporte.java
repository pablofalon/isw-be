package org.proyecto.business;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Component
public class ExcelExportadorReporte implements ExportadorReporte {

    private static final String[] ENCABEZADOS = {"", "Tickets", "Abiertos", "Cerrados", "Promedio de resolución (h)"};

    @Override
    public FormatoExportacion formato() {
        return FormatoExportacion.EXCEL;
    }

    @Override
    public byte[] exportar(Reporte reporte, boolean incluirTotales) {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet("Reporte");
            Font negrita = libro.createFont();
            negrita.setBold(true);
            CellStyle estiloEncabezado = libro.createCellStyle();
            estiloEncabezado.setFont(negrita);

            int fila = 0;
            fila = escribirTexto(hoja, fila, "Reporte de tickets", estiloEncabezado);
            fila = escribirTexto(hoja, fila, "Período: " + reporte.desde() + " a " + reporte.hasta(), null);
            fila = escribirTexto(hoja, fila, "Agrupado por: " + reporte.criterio(), null);
            fila++;

            Row encabezado = hoja.createRow(fila++);
            for (int i = 0; i < ENCABEZADOS.length; i++) {
                Cell celda = encabezado.createCell(i);
                celda.setCellValue(i == 0 ? reporte.criterio().name() : ENCABEZADOS[i]);
                celda.setCellStyle(estiloEncabezado);
            }
            for (Reporte.Grupo grupo : reporte.grupos()) {
                fila = escribirFila(hoja, fila, grupo.clave(), grupo.cantidad(), grupo.abiertos(), grupo.cerrados(),
                        grupo.promedioResolucionHoras(), null);
            }
            if (incluirTotales) {
                Reporte.Totales totales = reporte.totales();
                escribirFila(hoja, fila, "Total", totales.cantidad(), totales.abiertos(), totales.cerrados(),
                        totales.promedioResolucionHoras(), estiloEncabezado);
            }
            for (int i = 0; i < ENCABEZADOS.length; i++) {
                hoja.autoSizeColumn(i);
            }
            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new ExportacionReporteException("No se pudo generar el archivo Excel", e);
        }
    }

    private int escribirTexto(Sheet hoja, int fila, String texto, CellStyle estilo) {
        Cell celda = hoja.createRow(fila).createCell(0);
        celda.setCellValue(texto);
        if (estilo != null) {
            celda.setCellStyle(estilo);
        }
        return fila + 1;
    }

    private int escribirFila(Sheet hoja, int fila, String clave, long cantidad, long abiertos, long cerrados,
                             Double promedio, CellStyle estilo) {
        Row row = hoja.createRow(fila);
        row.createCell(0).setCellValue(clave);
        row.createCell(1).setCellValue(cantidad);
        row.createCell(2).setCellValue(abiertos);
        row.createCell(3).setCellValue(cerrados);
        if (promedio != null) {
            row.createCell(4).setCellValue(promedio);
        }
        if (estilo != null) {
            row.forEach(celda -> celda.setCellStyle(estilo));
        }
        return fila + 1;
    }
}

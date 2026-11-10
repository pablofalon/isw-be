package org.proyecto.business;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.Locale;

@Component
public class PdfExportadorReporte implements ExportadorReporte {

    private static final Font TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
    private static final Font NEGRITA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    private static final Font NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 10);

    @Override
    public FormatoExportacion formato() {
        return FormatoExportacion.PDF;
    }

    @Override
    public byte[] exportar(Reporte reporte, boolean incluirTotales) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4);
        try {
            PdfWriter.getInstance(documento, salida);
            documento.open();
            documento.add(new Paragraph("Reporte de tickets", TITULO));
            documento.add(new Paragraph("Período: " + reporte.desde() + " a " + reporte.hasta(), NORMAL));
            documento.add(new Paragraph("Agrupado por: " + reporte.criterio(), NORMAL));
            documento.add(new Paragraph(" "));

            PdfPTable tabla = new PdfPTable(new float[]{3, 1, 1, 1, 2});
            tabla.setWidthPercentage(100);
            for (String encabezado : new String[]{reporte.criterio().name(), "Tickets", "Abiertos", "Cerrados",
                    "Promedio de resolución (h)"}) {
                tabla.addCell(celda(encabezado, NEGRITA, Element.ALIGN_LEFT));
            }
            for (Reporte.Grupo grupo : reporte.grupos()) {
                agregarFila(tabla, grupo.clave(), grupo.cantidad(), grupo.abiertos(), grupo.cerrados(),
                        grupo.promedioResolucionHoras(), NORMAL);
            }
            if (incluirTotales) {
                Reporte.Totales totales = reporte.totales();
                agregarFila(tabla, "Total", totales.cantidad(), totales.abiertos(), totales.cerrados(),
                        totales.promedioResolucionHoras(), NEGRITA);
            }
            documento.add(tabla);
            documento.close();
            return salida.toByteArray();
        } catch (DocumentException e) {
            throw new ExportacionReporteException("No se pudo generar el archivo PDF", e);
        }
    }

    private void agregarFila(PdfPTable tabla, String clave, long cantidad, long abiertos, long cerrados,
                             Double promedio, Font fuente) {
        tabla.addCell(celda(clave, fuente, Element.ALIGN_LEFT));
        tabla.addCell(celda(String.valueOf(cantidad), fuente, Element.ALIGN_RIGHT));
        tabla.addCell(celda(String.valueOf(abiertos), fuente, Element.ALIGN_RIGHT));
        tabla.addCell(celda(String.valueOf(cerrados), fuente, Element.ALIGN_RIGHT));
        tabla.addCell(celda(promedio == null ? "-" : String.format(Locale.ROOT, "%.2f", promedio), fuente,
                Element.ALIGN_RIGHT));
    }

    private PdfPCell celda(String texto, Font fuente, int alineacion) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, fuente));
        celda.setHorizontalAlignment(alineacion);
        celda.setPadding(4);
        return celda;
    }
}

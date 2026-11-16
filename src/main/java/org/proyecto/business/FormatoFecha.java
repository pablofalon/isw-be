package org.proyecto.business;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Formato de las fechas que ve el usuario (reportes, exportaciones): DD/MM/AAAA. */
public final class FormatoFecha {

    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    /** Los nombres de archivo no admiten "/". */
    private static final DateTimeFormatter DIA_ARCHIVO = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private FormatoFecha() {
    }

    public static String dia(LocalDate fecha) {
        return fecha.format(DIA);
    }

    public static String diaParaArchivo(LocalDate fecha) {
        return fecha.format(DIA_ARCHIVO);
    }
}

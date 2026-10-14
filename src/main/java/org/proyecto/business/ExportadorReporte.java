package org.proyecto.business;

/** Estrategia de exportación de CU22: una implementación por formato. */
public interface ExportadorReporte {

    FormatoExportacion formato();

    byte[] exportar(Reporte reporte, boolean incluirTotales);
}

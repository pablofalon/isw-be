package org.proyecto.business;

import org.proyecto.entities.Ticket;

import java.util.List;

/**
 * Resultado de CU11. Los tiempos están en horas; no se persisten, se calculan al consultar.
 * promedioHoras es el tiempo de respuesta promedio de los tickets con prioridad (los demás no se calculan).
 */
public record MonitoreoTiempos(Resumen resumen, List<Item> items) {

    public record Resumen(long enPlazo, long proximosAVencer, long vencidos, long sinPrioridad, double promedioHoras) {
    }

    /** horasObjetivo y porcentajeConsumido son null cuando el ticket no tiene prioridad con objetivo. */
    public record Item(Ticket ticket, CumplimientoTiempo cumplimiento, double horasTranscurridas,
                       Integer horasObjetivo, Double porcentajeConsumido, boolean atendido) {
    }
}

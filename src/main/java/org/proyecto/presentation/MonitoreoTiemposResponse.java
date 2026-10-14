package org.proyecto.presentation;

import org.proyecto.business.CumplimientoTiempo;
import org.proyecto.business.MonitoreoTiempos;

import java.util.List;

/** CU11. Los tiempos están en horas. */
public record MonitoreoTiemposResponse(ResumenResponse resumen, List<ItemResponse> tickets) {

    public record ResumenResponse(long enPlazo, long proximosAVencer, long vencidos, long sinPrioridad,
                                  double promedioHoras) {
    }

    public record ItemResponse(TicketResponse ticket, CumplimientoTiempo cumplimiento, double horasTranscurridas,
                               Integer horasObjetivo, Double porcentajeConsumido, boolean atendido) {
    }

    public static MonitoreoTiemposResponse from(MonitoreoTiempos monitoreo) {
        MonitoreoTiempos.Resumen resumen = monitoreo.resumen();
        return new MonitoreoTiemposResponse(
                new ResumenResponse(resumen.enPlazo(), resumen.proximosAVencer(), resumen.vencidos(),
                        resumen.sinPrioridad(), resumen.promedioHoras()),
                monitoreo.items().stream()
                        .map(item -> new ItemResponse(TicketResponse.from(item.ticket()), item.cumplimiento(),
                                item.horasTranscurridas(), item.horasObjetivo(), item.porcentajeConsumido(),
                                item.atendido()))
                        .toList());
    }
}

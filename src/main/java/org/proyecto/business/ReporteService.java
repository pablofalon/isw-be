package org.proyecto.business;

import org.proyecto.data.TicketRepository;
import org.proyecto.entities.Ticket;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Reportes e indicadores de gestión (CU20, CU21, CU22). Son de solo lectura: se calculan al
 * consultar sobre los tickets creados en el período y no se persisten (por eso la exportación
 * vuelve a calcular el reporte con los mismos parámetros). Abierto/cerrado sale de
 * TicketEstado.estadoFinal, igual que "operativo" en CU10.
 */
@Service
public class ReporteService {

    /** CU21: cuántas categorías se informan como incidencias recurrentes. */
    private static final int CANTIDAD_RECURRENTES = 3;

    private final TicketRepository ticketRepository;
    private final Map<FormatoExportacion, ExportadorReporte> exportadores = new EnumMap<>(FormatoExportacion.class);

    public ReporteService(TicketRepository ticketRepository, List<ExportadorReporte> exportadores) {
        this.ticketRepository = ticketRepository;
        exportadores.forEach(exportador -> this.exportadores.put(exportador.formato(), exportador));
    }

    // ---- CU20 - Generar reportes ----
    @Transactional(readOnly = true)
    public Reporte generarReporte(LocalDate desde, LocalDate hasta, CriterioAgrupamiento criterio,
                                  FiltroReporte filtro) {
        validarPeriodoObligatorio(desde, hasta);
        if (criterio == null) {
            throw new InvalidTicketOperationException("El criterio de agrupamiento es obligatorio");
        }
        List<Ticket> tickets = buscarTickets(desde, hasta, filtro);

        List<Reporte.Grupo> grupos = tickets.stream()
                .collect(Collectors.groupingBy(ticket -> claveDe(criterio, ticket), LinkedHashMap::new,
                        Collectors.toList()))
                .entrySet().stream()
                .map(entrada -> armarGrupo(entrada.getKey(), entrada.getValue()))
                .sorted(Comparator.comparingLong(Reporte.Grupo::cantidad).reversed()
                        .thenComparing(Reporte.Grupo::clave))
                .toList();
        Reporte.Grupo total = armarGrupo("Total", tickets);
        return new Reporte(desde, hasta, criterio, grupos,
                new Reporte.Totales(total.cantidad(), total.abiertos(), total.cerrados(), total.promedioResolucionHoras()));
    }

    // ---- CU21 - Consultar indicadores ----

    /** Sin período informado se usa el mes en curso (CU21 paso 2). */
    @Transactional(readOnly = true)
    public Indicadores calcularIndicadores(LocalDate desde, LocalDate hasta) {
        LocalDate hoy = LocalDate.now();
        LocalDate desdeEfectivo = desde != null ? desde : hoy.withDayOfMonth(1);
        LocalDate hastaEfectivo = hasta != null ? hasta : hoy;
        validarPeriodoObligatorio(desdeEfectivo, hastaEfectivo);

        List<Ticket> tickets = buscarTickets(desdeEfectivo, hastaEfectivo, FiltroReporte.sinFiltros());
        Reporte.Grupo total = armarGrupo("Total", tickets);
        List<Indicadores.Distribucion> porCategoria = distribuir(tickets, ticket -> claveDe(CriterioAgrupamiento.CATEGORIA, ticket));
        List<Indicadores.Distribucion> porPrioridad = distribuir(tickets, ticket -> claveDe(CriterioAgrupamiento.PRIORIDAD, ticket));
        List<Indicadores.Distribucion> recurrentes = porCategoria.stream()
                .filter(distribucion -> !SIN_CATEGORIA.equals(distribucion.clave()))
                .limit(CANTIDAD_RECURRENTES)
                .toList();
        return new Indicadores(desdeEfectivo, hastaEfectivo, total.cantidad(), total.abiertos(), total.cerrados(),
                total.promedioResolucionHoras() != null ? total.promedioResolucionHoras() : 0.0,
                porCategoria, porPrioridad, recurrentes);
    }

    // ---- CU22 - Exportar reportes ----
    @Transactional(readOnly = true)
    public ArchivoExportado exportarReporte(LocalDate desde, LocalDate hasta, CriterioAgrupamiento criterio,
                                            FiltroReporte filtro, FormatoExportacion formato, boolean incluirTotales) {
        ExportadorReporte exportador = formato == null ? null : exportadores.get(formato);
        if (exportador == null) {
            throw new InvalidTicketOperationException("Seleccioná un formato de exportación válido (PDF, Excel o CSV)");
        }
        Reporte reporte = generarReporte(desde, hasta, criterio, filtro);
        if (reporte.sinDatos()) {
            throw new InvalidTicketOperationException("No hay datos para exportar en el período seleccionado");
        }
        byte[] contenido = exportador.exportar(reporte, incluirTotales);
        String nombre = "reporte-tickets-" + LocalDate.now() + "." + formato.getExtension();
        return new ArchivoExportado(nombre, contenido, formato.getTipoContenido());
    }

    // ---- Helpers ----

    private static final String SIN_CATEGORIA = "Sin categoría";

    private List<Ticket> buscarTickets(LocalDate desde, LocalDate hasta, FiltroReporte filtro) {
        return ticketRepository.buscarPorPeriodo(desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay(),
                filtro.idCategoria(), filtro.idPrioridad(), filtro.estado(), filtro.idOperador());
    }

    /** CU20 paso 5: desde no posterior a hasta y sin fechas futuras. */
    private void validarPeriodoObligatorio(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new InvalidTicketOperationException("El período (desde y hasta) es obligatorio");
        }
        if (desde.isAfter(hasta)) {
            throw new InvalidTicketOperationException("La fecha desde no puede ser posterior a la fecha hasta");
        }
        if (hasta.isAfter(LocalDate.now())) {
            throw new InvalidTicketOperationException("El período no puede incluir fechas futuras");
        }
    }

    private String claveDe(CriterioAgrupamiento criterio, Ticket ticket) {
        return switch (criterio) {
            case CATEGORIA -> ticket.getCategoria() != null ? ticket.getCategoria().getNombre() : SIN_CATEGORIA;
            case PRIORIDAD -> ticket.getPrioridad() != null ? ticket.getPrioridad().getNivel().name() : "Sin prioridad";
            case ESTADO -> ticket.getEstadoActual().getNombre();
            case OPERADOR -> ticket.getOperador() != null
                    ? ticket.getOperador().getNombre() + " " + ticket.getOperador().getApellido()
                    : "Sin asignar";
        };
    }

    private Reporte.Grupo armarGrupo(String clave, List<Ticket> tickets) {
        long cerrados = tickets.stream().filter(ticket -> ticket.getEstadoActual().isEstadoFinal()).count();
        double promedio = tickets.stream()
                .filter(ticket -> ticket.getEstadoActual().isEstadoFinal() && ticket.getFechaCierre() != null)
                .mapToDouble(ReporteService::horasDeResolucion)
                .average().orElse(Double.NaN);
        return new Reporte.Grupo(clave, tickets.size(), tickets.size() - cerrados, cerrados,
                Double.isNaN(promedio) ? null : Math.round(promedio * 100.0) / 100.0);
    }

    /** Tiempo de resolución: de la creación al cierre, en horas. */
    private static double horasDeResolucion(Ticket ticket) {
        return Duration.between(ticket.getFechaCreacion(), ticket.getFechaCierre()).toMinutes() / 60.0;
    }

    /** Cantidad de tickets por clave, de mayor a menor. */
    private List<Indicadores.Distribucion> distribuir(List<Ticket> tickets, Function<Ticket, String> clave) {
        return tickets.stream()
                .collect(Collectors.groupingBy(clave, Collectors.counting()))
                .entrySet().stream()
                .map(entrada -> new Indicadores.Distribucion(entrada.getKey(), entrada.getValue()))
                .sorted(Comparator.comparingLong(Indicadores.Distribucion::cantidad).reversed()
                        .thenComparing(Indicadores.Distribucion::clave))
                .toList();
    }
}

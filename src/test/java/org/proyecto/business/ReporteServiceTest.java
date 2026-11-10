package org.proyecto.business;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.proyecto.data.TicketRepository;
import org.proyecto.entities.Categoria;
import org.proyecto.entities.NivelPrioridad;
import org.proyecto.entities.Operador;
import org.proyecto.entities.Prioridad;
import org.proyecto.entities.Ticket;
import org.proyecto.entities.TicketEstado;
import org.proyecto.entities.TicketEstadoCodigo;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    @Mock private TicketRepository ticketRepository;

    private ReporteService reporteService;
    private LocalDate hoy;

    @BeforeEach
    void setUp() {
        reporteService = new ReporteService(ticketRepository, List.of(new CsvExportadorReporte(),
                new ExcelExportadorReporte(), new PdfExportadorReporte()));
        hoy = LocalDate.now();
    }

    private Ticket ticket(String categoria, NivelPrioridad prioridad, TicketEstadoCodigo estado, boolean final_,
                          String operador, int horasHastaCierre) {
        Ticket ticket = new Ticket();
        ticket.setFechaCreacion(hoy.minusDays(3).atTime(9, 0));
        ticket.setEstadoActual(new TicketEstado(estado, estado.name(), null, final_, false));
        if (categoria != null) {
            ticket.setCategoria(new Categoria(categoria, null));
        }
        if (prioridad != null) {
            ticket.setPrioridad(new Prioridad(prioridad, null, 24));
        }
        if (operador != null) {
            Operador asignado = new Operador();
            asignado.setNombre(operador);
            asignado.setApellido("Soporte");
            ticket.setOperador(asignado);
        }
        if (final_) {
            ticket.setFechaCierre(ticket.getFechaCreacion().plusHours(horasHastaCierre));
        }
        return ticket;
    }

    private List<Ticket> ticketsDeEjemplo() {
        return List.of(
                ticket("Acceso", NivelPrioridad.ALTA, TicketEstadoCodigo.CERRADO, true, "Ana", 10),
                ticket("Acceso", NivelPrioridad.ALTA, TicketEstadoCodigo.CERRADO, true, "Ana", 20),
                ticket("Acceso", NivelPrioridad.MEDIA, TicketEstadoCodigo.EN_ANALISIS, false, "Beto", 0),
                ticket("Reportes", null, TicketEstadoCodigo.NUEVO, false, null, 0));
    }

    private void hayTickets(List<Ticket> tickets) {
        when(ticketRepository.buscarPorPeriodo(any(), any(), any(), any(), any(), any())).thenReturn(tickets);
    }

    @Test
    void generarReporteAgrupaPorCategoriaYCalculaTotalesYPromedioSobreLosCerrados() {
        hayTickets(ticketsDeEjemplo());

        Reporte reporte = reporteService.generarReporte(hoy.minusDays(7), hoy, CriterioAgrupamiento.CATEGORIA,
                FiltroReporte.sinFiltros());

        assertEquals(List.of("Acceso", "Reportes"), reporte.grupos().stream().map(Reporte.Grupo::clave).toList());
        Reporte.Grupo acceso = reporte.grupos().get(0);
        assertEquals(3, acceso.cantidad());
        assertEquals(1, acceso.abiertos());
        assertEquals(2, acceso.cerrados());
        assertEquals(15.0, acceso.promedioResolucionHoras());
        assertNull(reporte.grupos().get(1).promedioResolucionHoras());
        assertEquals(4, reporte.totales().cantidad());
        assertEquals(2, reporte.totales().abiertos());
        assertEquals(15.0, reporte.totales().promedioResolucionHoras());
        assertFalse(reporte.sinDatos());
    }

    @Test
    void generarReporteAgrupaPorPrioridadOperadorYEstadoConClaveParaLosQueNoTienen() {
        hayTickets(ticketsDeEjemplo());

        Reporte porPrioridad = reporteService.generarReporte(hoy.minusDays(7), hoy, CriterioAgrupamiento.PRIORIDAD,
                FiltroReporte.sinFiltros());
        assertEquals(List.of("ALTA", "MEDIA", "Sin prioridad"), porPrioridad.grupos().stream().map(Reporte.Grupo::clave).toList());

        Reporte porOperador = reporteService.generarReporte(hoy.minusDays(7), hoy, CriterioAgrupamiento.OPERADOR,
                FiltroReporte.sinFiltros());
        assertEquals(List.of("Ana Soporte", "Beto Soporte", "Sin asignar"),
                porOperador.grupos().stream().map(Reporte.Grupo::clave).toList());

        Reporte porEstado = reporteService.generarReporte(hoy.minusDays(7), hoy, CriterioAgrupamiento.ESTADO,
                FiltroReporte.sinFiltros());
        assertEquals("CERRADO", porEstado.grupos().get(0).clave());
    }

    @Test
    void generarReporteTraduceElPeriodoAlRangoCompletoDeDiasYPasaLosFiltros() {
        hayTickets(List.of());

        reporteService.generarReporte(hoy.minusDays(7), hoy, CriterioAgrupamiento.CATEGORIA,
                new FiltroReporte(5L, 6L, TicketEstadoCodigo.CERRADO, 7L));

        verify(ticketRepository).buscarPorPeriodo(hoy.minusDays(7).atStartOfDay(), hoy.plusDays(1).atStartOfDay(),
                5L, 6L, TicketEstadoCodigo.CERRADO, 7L);
    }

    @Test
    void generarReporteSinTicketsDevuelveReporteSinDatos() {
        hayTickets(List.of());

        Reporte reporte = reporteService.generarReporte(hoy.minusDays(7), hoy, CriterioAgrupamiento.CATEGORIA,
                FiltroReporte.sinFiltros());

        assertTrue(reporte.sinDatos());
        assertTrue(reporte.grupos().isEmpty());
    }

    @Test
    void generarReporteConPeriodoInvalidoFallaSinConsultar() {
        assertThrows(InvalidTicketOperationException.class, () -> reporteService.generarReporte(
                hoy, hoy.minusDays(1), CriterioAgrupamiento.CATEGORIA, FiltroReporte.sinFiltros()));
        assertThrows(InvalidTicketOperationException.class, () -> reporteService.generarReporte(
                hoy, hoy.plusDays(1), CriterioAgrupamiento.CATEGORIA, FiltroReporte.sinFiltros()));
        assertThrows(InvalidTicketOperationException.class, () -> reporteService.generarReporte(
                null, hoy, CriterioAgrupamiento.CATEGORIA, FiltroReporte.sinFiltros()));
        assertThrows(InvalidTicketOperationException.class, () -> reporteService.generarReporte(
                hoy.minusDays(1), hoy, null, FiltroReporte.sinFiltros()));
        verify(ticketRepository, never()).buscarPorPeriodo(any(), any(), any(), any(), any(), any());
    }

    @Test
    void calcularIndicadoresArmaTotalesDistribucionesYLasTresCategoriasRecurrentes() {
        List<Ticket> tickets = new java.util.ArrayList<>(ticketsDeEjemplo());
        tickets.add(ticket("Rendimiento", NivelPrioridad.BAJA, TicketEstadoCodigo.NUEVO, false, null, 0));
        tickets.add(ticket("Rendimiento", NivelPrioridad.BAJA, TicketEstadoCodigo.NUEVO, false, null, 0));
        tickets.add(ticket("Notificaciones", NivelPrioridad.BAJA, TicketEstadoCodigo.NUEVO, false, null, 0));
        tickets.add(ticket(null, null, TicketEstadoCodigo.NUEVO, false, null, 0));
        hayTickets(tickets);

        Indicadores indicadores = reporteService.calcularIndicadores(hoy.minusDays(7), hoy);

        assertEquals(8, indicadores.total());
        assertEquals(6, indicadores.abiertos());
        assertEquals(2, indicadores.cerrados());
        assertEquals(15.0, indicadores.promedioResolucionHoras());
        assertEquals("Acceso", indicadores.porCategoria().get(0).clave());
        assertEquals(3, indicadores.porCategoria().get(0).cantidad());
        // La más frecuente primero; los tickets sin categoría no cuentan como incidencia recurrente.
        assertEquals(List.of("Acceso", "Rendimiento", "Notificaciones"),
                indicadores.recurrentes().stream().map(Indicadores.Distribucion::clave).toList());
        assertFalse(indicadores.sinDatos());
    }

    @Test
    void calcularIndicadoresSinPeriodoUsaElMesEnCursoYSinTicketsQuedaEnCero() {
        hayTickets(List.of());

        Indicadores indicadores = reporteService.calcularIndicadores(null, null);

        assertEquals(hoy.withDayOfMonth(1), indicadores.desde());
        assertEquals(hoy, indicadores.hasta());
        assertTrue(indicadores.sinDatos());
        assertEquals(0, indicadores.abiertos());
        assertEquals(0.0, indicadores.promedioResolucionHoras());
        assertTrue(indicadores.recurrentes().isEmpty());
    }

    @Test
    void calcularIndicadoresConPeriodoInvalidoFalla() {
        assertThrows(InvalidTicketOperationException.class,
                () -> reporteService.calcularIndicadores(hoy, hoy.minusDays(2)));
    }

    @Test
    void exportarReporteGeneraElArchivoConNombreYTipoDelFormato() {
        hayTickets(ticketsDeEjemplo());

        ArchivoExportado csv = reporteService.exportarReporte(hoy.minusDays(7), hoy, CriterioAgrupamiento.CATEGORIA,
                FiltroReporte.sinFiltros(), FormatoExportacion.CSV, true);
        ArchivoExportado excel = reporteService.exportarReporte(hoy.minusDays(7), hoy, CriterioAgrupamiento.CATEGORIA,
                FiltroReporte.sinFiltros(), FormatoExportacion.EXCEL, true);
        ArchivoExportado pdf = reporteService.exportarReporte(hoy.minusDays(7), hoy, CriterioAgrupamiento.CATEGORIA,
                FiltroReporte.sinFiltros(), FormatoExportacion.PDF, false);

        assertEquals("reporte-tickets-" + hoy + ".csv", csv.nombre());
        assertEquals("reporte-tickets-" + hoy + ".xlsx", excel.nombre());
        assertEquals("reporte-tickets-" + hoy + ".pdf", pdf.nombre());
        // Firmas de cada formato: xlsx es un zip (PK) y el PDF empieza con %PDF.
        assertEquals("PK", new String(excel.contenido(), 0, 2, StandardCharsets.ISO_8859_1));
        assertEquals("%PDF", new String(pdf.contenido(), 0, 4, StandardCharsets.ISO_8859_1));
    }

    @Test
    void exportarReporteSinFormatoOSinDatosFalla() {
        assertThrows(InvalidTicketOperationException.class, () -> reporteService.exportarReporte(
                hoy.minusDays(7), hoy, CriterioAgrupamiento.CATEGORIA, FiltroReporte.sinFiltros(), null, true));
        verify(ticketRepository, never()).buscarPorPeriodo(any(), any(), any(), any(), any(), any());

        hayTickets(List.of());
        assertThrows(InvalidTicketOperationException.class, () -> reporteService.exportarReporte(
                hoy.minusDays(7), hoy, CriterioAgrupamiento.CATEGORIA, FiltroReporte.sinFiltros(),
                FormatoExportacion.CSV, true));
    }

    @Test
    void elCsvIncluyeLosTotalesSoloSiSePide() {
        hayTickets(ticketsDeEjemplo());

        String conTotales = csv(true);
        String sinTotales = csv(false);

        assertTrue(conTotales.contains("Total,4,2,2,15.00"));
        assertFalse(sinTotales.contains("Total,"));
        assertTrue(sinTotales.contains("Acceso,3,1,2,15.00"));
        assertTrue(sinTotales.contains("Reportes,1,1,0,"));
    }

    private String csv(boolean incluirTotales) {
        Reporte reporte = reporteService.generarReporte(hoy.minusDays(7), hoy, CriterioAgrupamiento.CATEGORIA,
                FiltroReporte.sinFiltros());
        return new String(new CsvExportadorReporte().exportar(reporte, incluirTotales), StandardCharsets.UTF_8);
    }
}

package org.proyecto.presentation;

import org.proyecto.business.ArchivoExportado;
import org.proyecto.business.Autorizador;
import org.proyecto.business.CriterioAgrupamiento;
import org.proyecto.business.FiltroReporte;
import org.proyecto.business.FormatoExportacion;
import org.proyecto.business.Permisos;
import org.proyecto.business.ReporteService;
import org.proyecto.business.SesionActiva;
import org.proyecto.config.AuthenticatedSession;
import org.proyecto.entities.TicketEstadoCodigo;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private final ReporteService reporteService;
    private final Autorizador autorizador;

    public ReporteController(ReporteService reporteService, Autorizador autorizador) {
        this.reporteService = reporteService;
        this.autorizador = autorizador;
    }

    // CU20
    @GetMapping
    public ReporteResponse generar(@AuthenticatedSession SesionActiva sesion,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                   @RequestParam(required = false) CriterioAgrupamiento criterio,
                                   @RequestParam(required = false) Long idCategoria,
                                   @RequestParam(required = false) Long idPrioridad,
                                   @RequestParam(required = false) TicketEstadoCodigo estado,
                                   @RequestParam(required = false) Long idOperador) {
        autorizador.requirePermiso(sesion, Permisos.REPORTES_CONSULTAR);
        return ReporteResponse.from(reporteService.generarReporte(desde, hasta, criterio,
                new FiltroReporte(idCategoria, idPrioridad, estado, idOperador)));
    }

    // CU21
    @GetMapping("/indicadores")
    public IndicadoresResponse indicadores(@AuthenticatedSession SesionActiva sesion,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        autorizador.requirePermiso(sesion, Permisos.REPORTES_CONSULTAR);
        return IndicadoresResponse.from(reporteService.calcularIndicadores(desde, hasta));
    }

    // CU22
    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportar(@AuthenticatedSession SesionActiva sesion,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                           @RequestParam(required = false) CriterioAgrupamiento criterio,
                                           @RequestParam(required = false) Long idCategoria,
                                           @RequestParam(required = false) Long idPrioridad,
                                           @RequestParam(required = false) TicketEstadoCodigo estado,
                                           @RequestParam(required = false) Long idOperador,
                                           @RequestParam(required = false) FormatoExportacion formato,
                                           @RequestParam(defaultValue = "true") boolean incluirTotales) {
        autorizador.requirePermiso(sesion, Permisos.REPORTES_CONSULTAR);
        ArchivoExportado archivo = reporteService.exportarReporte(desde, hasta, criterio,
                new FiltroReporte(idCategoria, idPrioridad, estado, idOperador), formato, incluirTotales);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(archivo.nombre()).build().toString())
                .contentType(MediaType.parseMediaType(archivo.tipoContenido()))
                .body(archivo.contenido());
    }
}

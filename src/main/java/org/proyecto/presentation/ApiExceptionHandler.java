package org.proyecto.presentation;

import org.proyecto.business.CuentaNoDisponibleException;
import org.proyecto.business.ExportacionReporteException;
import org.proyecto.business.ForbiddenOperationException;
import org.proyecto.business.InvalidTicketOperationException;
import org.proyecto.business.LimitesAdjuntos;
import org.proyecto.business.OperacionSeguridadInvalidaException;
import org.proyecto.business.RecursoNoEncontradoException;
import org.proyecto.business.TicketNotFoundException;
import org.proyecto.entities.ComposicionInvalidaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private final LimitesAdjuntos limitesAdjuntos;

    public ApiExceptionHandler(LimitesAdjuntos limitesAdjuntos) {
        this.limitesAdjuntos = limitesAdjuntos;
    }

    @ExceptionHandler(TicketNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(TicketNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(InvalidTicketOperationException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOperation(InvalidTicketOperationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenOperationException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> handleRecursoNoEncontrado(RecursoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler({OperacionSeguridadInvalidaException.class, ComposicionInvalidaException.class})
    public ResponseEntity<ErrorResponse> handleSeguridadInvalida(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(ex.getMessage()));
    }

    /** CU22 E1: falló la generación del archivo; la interfaz ofrece reintentar o elegir otro formato. */
    @ExceptionHandler(ExportacionReporteException.class)
    public ResponseEntity<ErrorResponse> handleExportacionFallida(ExportacionReporteException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(CuentaNoDisponibleException.class)
    public ResponseEntity<ErrorResponse> handleCuentaNoDisponible(CuentaNoDisponibleException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(ex.getMessage()));
    }

    /** Spring corta el multipart antes de llegar al servicio; se responde con el mismo mensaje de CU02 FA2. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleArchivoDemasiadoGrande(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(
                "El archivo supera el tamaño máximo permitido (" + limitesAdjuntos.getTamanioMaximoMb() + " MB)"));
    }
}

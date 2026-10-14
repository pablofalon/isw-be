package org.proyecto.entities;

/**
 * Qué representa una entrada de HistorialEstado: un cambio de estado, una actualización que no
 * cambia el estado (clasificación, prioridad, adjuntos, validación de resolución) o una acción del
 * operador registrada con CU08. Las filas anteriores a este campo quedan en null y se leen como
 * CAMBIO_ESTADO.
 */
public enum TipoRegistroHistorial {
    CAMBIO_ESTADO,
    ACTUALIZACION,
    ACCION
}

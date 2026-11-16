package org.proyecto.presentation;

/** Modificación de una prioridad (CU25): descripción y tiempo máximo de respuesta en horas. */
public record ModificarPrioridadRequest(String descripcion, Integer tiempoObjetivoRespuesta) {
}

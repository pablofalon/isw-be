package org.proyecto.entities;

/**
 * Códigos de estado del ticket según el diagrama de transición de estados del documento
 * (Recepción de reclamos + Cierre de tickets). TicketEstado es la entidad persistida
 * (catálogo), este enum identifica cada fila de forma estable en el código.
 */
public enum TicketEstadoCodigo {
    NUEVO,
    PENDIENTE_CORRECCION,
    VALIDADO,
    EN_ANALISIS,
    RESUELTO,
    CERRADO,
    REABIERTO
}

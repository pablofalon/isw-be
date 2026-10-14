package org.proyecto.entities;

/**
 * Ciclo de vida de una notificación (CU16): se registra PENDIENTE, pasa a ENVIADA cuando el canal
 * la entrega o a FALLIDA si falla (se reintenta en diferido), y a LEIDA cuando el destinatario la
 * abre en su panel.
 */
public enum EstadoNotificacion {
    PENDIENTE,
    ENVIADA,
    FALLIDA,
    LEIDA
}

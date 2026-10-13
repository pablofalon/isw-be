package org.proyecto.business;

import org.proyecto.entities.Notificacion;

/**
 * Canal por el que se entrega una notificación (CU16 paso 5). Si la entrega falla, el canal lanza
 * una excepción y NotificacionService marca la notificación como FALLIDA para reintentarla.
 */
public interface CanalNotificacion {

    String nombre();

    void enviar(Notificacion notificacion);
}

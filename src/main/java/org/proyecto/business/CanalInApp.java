package org.proyecto.business;

import org.proyecto.entities.Notificacion;
import org.springframework.stereotype.Component;

/**
 * Único canal del alcance actual: la notificación se entrega en el panel del destinatario dentro
 * del sistema. Los canales externos (correo, WhatsApp) quedan excluidos (G02 - Exclusiones); para
 * sumarlos alcanza con otra implementación de CanalNotificacion.
 */
@Component
public class CanalInApp implements CanalNotificacion {

    @Override
    public String nombre() {
        return "IN_APP";
    }

    @Override
    public void enviar(Notificacion notificacion) {
        // Ya persistida y asociada al destinatario: el panel la muestra sin otro paso de entrega.
    }
}

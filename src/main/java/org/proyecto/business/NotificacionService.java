package org.proyecto.business;

import org.proyecto.data.NotificacionRepository;
import org.proyecto.entities.Notificacion;
import org.proyecto.entities.Operador;
import org.proyecto.entities.UsuarioTicket;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * El vínculo opcional Notificacion.historialEstado (DER) no se completa acá a propósito: el
 * HistorialEstado recién creado en la misma transacción del Ticket puede llegar como instancia
 * detached/transient según cómo Hibernate resuelva el merge, y no aporta nada funcional todavía
 * (no hay pantalla de notificaciones en el alcance actual) frente al riesgo de acoplar el flush.
 */
@Service
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;

    public NotificacionService(NotificacionRepository notificacionRepository) {
        this.notificacionRepository = notificacionRepository;
    }

    public void notificarUsuario(UsuarioTicket destinatario, String mensaje) {
        Notificacion notificacion = new Notificacion();
        notificacion.setMensaje(mensaje);
        notificacion.setFechaEnvio(LocalDateTime.now());
        notificacion.setDestinatarioUsuario(destinatario);
        notificacionRepository.save(notificacion);
    }

    public void notificarOperador(Operador destinatario, String mensaje) {
        Notificacion notificacion = new Notificacion();
        notificacion.setMensaje(mensaje);
        notificacion.setFechaEnvio(LocalDateTime.now());
        notificacion.setDestinatarioOperador(destinatario);
        notificacionRepository.save(notificacion);
    }
}

package org.proyecto.business;

import org.proyecto.data.NotificacionRepository;
import org.proyecto.entities.CuentaAcceso;
import org.proyecto.entities.EstadoNotificacion;
import org.proyecto.entities.Notificacion;
import org.proyecto.entities.Operador;
import org.proyecto.entities.Ticket;
import org.proyecto.entities.UsuarioTicket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * CU16 - Notificar actualización. Cada notificación se registra PENDIENTE, se entrega por el canal
 * configurado y queda ENVIADA o FALLIDA; las fallidas se reintentan en diferido hasta agotar los
 * intentos (FA1, E2). Una falla al notificar nunca interrumpe la operación que la originó
 * (postcondición 3).
 *
 * El vínculo opcional Notificacion.historialEstado (DER) no se completa acá a propósito: el
 * HistorialEstado recién creado en la misma transacción del Ticket puede llegar como instancia
 * detached/transient según cómo Hibernate resuelva el merge, y el ticket ya identifica el caso.
 */
@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

    /** Largo de la columna Notificacion.mensaje: los textos libres (solución, motivo) pueden ser más largos. */
    private static final int LARGO_MAXIMO_MENSAJE = 500;

    private final NotificacionRepository notificacionRepository;
    private final CanalNotificacion canal;
    private final int maximoIntentos;

    public NotificacionService(NotificacionRepository notificacionRepository, CanalNotificacion canal,
                               @Value("${app.notificaciones.maximo-intentos:3}") int maximoIntentos) {
        this.notificacionRepository = notificacionRepository;
        this.canal = canal;
        this.maximoIntentos = maximoIntentos;
    }

    public void notificarUsuario(Ticket ticket, UsuarioTicket destinatario, String mensaje) {
        notificar(ticket, destinatario, null, mensaje);
    }

    public void notificarOperador(Ticket ticket, Operador destinatario, String mensaje) {
        notificar(ticket, null, destinatario, mensaje);
    }

    private void notificar(Ticket ticket, UsuarioTicket usuario, Operador operador, String mensaje) {
        try {
            Notificacion notificacion = new Notificacion();
            notificacion.setTicket(ticket);
            notificacion.setDestinatarioUsuario(usuario);
            notificacion.setDestinatarioOperador(operador);
            notificacion.setMensaje(recortar(mensaje));
            notificacion.setTipoCanal(canal.nombre());
            notificacion.setEstadoEnvio(EstadoNotificacion.PENDIENTE);
            notificacion.setIntentos(0);
            notificacion.setFechaEnvio(LocalDateTime.now());
            enviar(notificacionRepository.save(notificacion));
        } catch (RuntimeException e) {
            // E1: no se pudo ni registrar; se deja constancia y la operación del ticket sigue.
            log.error("No se pudo registrar la notificación del ticket #{}", ticket.getId(), e);
        }
    }

    /** Pasos 5 y 6 (o FA1): entrega por el canal y deja el resultado registrado. */
    private void enviar(Notificacion notificacion) {
        notificacion.setIntentos(notificacion.getIntentos() + 1);
        notificacion.setFechaEnvio(LocalDateTime.now());
        try {
            canal.enviar(notificacion);
            notificacion.setEstadoEnvio(EstadoNotificacion.ENVIADA);
        } catch (RuntimeException e) {
            notificacion.setEstadoEnvio(EstadoNotificacion.FALLIDA);
            log.warn("Falló el envío de la notificación {} (intento {} de {})",
                    notificacion.getId(), notificacion.getIntentos(), maximoIntentos, e);
        }
        notificacionRepository.save(notificacion);
    }

    /** FA1: reintento diferido de las fallidas. Con los intentos agotados quedan FALLIDA para revisión (E2). */
    @Scheduled(fixedDelayString = "${app.notificaciones.reintento-ms:60000}")
    public void reintentarFallidas() {
        for (Notificacion notificacion : notificacionRepository.findByEstadoEnvio(EstadoNotificacion.FALLIDA)) {
            if (notificacion.getIntentos() < maximoIntentos) {
                enviar(notificacion);
            }
        }
    }

    // ---- Panel del destinatario (postcondición 2) ----

    public List<Notificacion> listarDe(CuentaAcceso cuenta) {
        if (cuenta.getUsuarioTicket() != null) {
            return notificacionRepository.findByDestinatarioUsuarioIdOrderByIdDesc(cuenta.getUsuarioTicket().getId());
        }
        if (cuenta.getOperador() != null) {
            return notificacionRepository.findByDestinatarioOperadorIdOrderByIdDesc(cuenta.getOperador().getId());
        }
        return List.of();
    }

    public long contarNoLeidas(CuentaAcceso cuenta) {
        return listarDe(cuenta).stream().filter(n -> n.getEstadoEnvio() != EstadoNotificacion.LEIDA).count();
    }

    public Notificacion marcarLeida(Long notificacionId, CuentaAcceso cuenta) {
        Notificacion notificacion = notificacionRepository.findById(notificacionId)
                .filter(n -> esDestinatario(n, cuenta))
                .orElseThrow(() -> new RecursoNoEncontradoException("Notificación inexistente: " + notificacionId));
        return leer(notificacion);
    }

    public void marcarTodasLeidas(CuentaAcceso cuenta) {
        listarDe(cuenta).stream()
                .filter(n -> n.getEstadoEnvio() != EstadoNotificacion.LEIDA)
                .forEach(this::leer);
    }

    private Notificacion leer(Notificacion notificacion) {
        if (notificacion.getEstadoEnvio() != EstadoNotificacion.LEIDA) {
            notificacion.setEstadoEnvio(EstadoNotificacion.LEIDA);
            notificacion.setFechaLectura(LocalDateTime.now());
            notificacion = notificacionRepository.save(notificacion);
        }
        return notificacion;
    }

    private boolean esDestinatario(Notificacion notificacion, CuentaAcceso cuenta) {
        UsuarioTicket usuario = notificacion.getDestinatarioUsuario();
        Operador operador = notificacion.getDestinatarioOperador();
        return (usuario != null && cuenta.getUsuarioTicket() != null && usuario.getId().equals(cuenta.getUsuarioTicket().getId()))
                || (operador != null && cuenta.getOperador() != null && operador.getId().equals(cuenta.getOperador().getId()));
    }

    private String recortar(String mensaje) {
        return mensaje.length() <= LARGO_MAXIMO_MENSAJE ? mensaje : mensaje.substring(0, LARGO_MAXIMO_MENSAJE - 3) + "...";
    }
}

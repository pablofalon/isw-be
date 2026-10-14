package org.proyecto.presentation;

import org.proyecto.entities.EstadoNotificacion;
import org.proyecto.entities.Notificacion;

import java.time.LocalDateTime;

public class NotificacionResponse {

    private Long id;
    private String mensaje;
    private EstadoNotificacion estado;
    private LocalDateTime fechaEnvio;
    private LocalDateTime fechaLectura;
    private Long ticketId;

    public NotificacionResponse() {
    }

    public static NotificacionResponse from(Notificacion notificacion) {
        NotificacionResponse response = new NotificacionResponse();
        response.id = notificacion.getId();
        response.mensaje = notificacion.getMensaje();
        response.estado = notificacion.getEstadoEnvio();
        response.fechaEnvio = notificacion.getFechaEnvio();
        response.fechaLectura = notificacion.getFechaLectura();
        response.ticketId = notificacion.getTicket() != null ? notificacion.getTicket().getId() : null;
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public EstadoNotificacion getEstado() {
        return estado;
    }

    public void setEstado(EstadoNotificacion estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaEnvio() {
        return fechaEnvio;
    }

    public void setFechaEnvio(LocalDateTime fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }

    public LocalDateTime getFechaLectura() {
        return fechaLectura;
    }

    public void setFechaLectura(LocalDateTime fechaLectura) {
        this.fechaLectura = fechaLectura;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }
}

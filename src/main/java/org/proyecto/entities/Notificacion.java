package org.proyecto.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Notificación de CU16 (RF16). El alcance del proyecto excluye integraciones con canales externos
 * (ver G02 - Exclusiones), por lo que "tipoCanal" hoy solo persiste IN_APP. "fechaEnvio" guarda el
 * momento del último intento de entrega.
 */
@Entity
@Table(name = "notificaciones")
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String mensaje;

    @Column(nullable = false)
    private String tipoCanal = "IN_APP";

    @Column(nullable = false)
    private LocalDateTime fechaEnvio;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EstadoNotificacion estadoEnvio = EstadoNotificacion.PENDIENTE;

    /** Intentos de entrega realizados (CU16 FA1). Las filas anteriores a este campo quedan en null. */
    private Integer intentos;

    /** Ticket sobre el que informa la notificación (CU16 postcondición 1). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id")
    private Ticket ticket;

    private LocalDateTime fechaLectura;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "historial_estado_id")
    private HistorialEstado historialEstado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destinatario_usuario_id")
    private UsuarioTicket destinatarioUsuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destinatario_operador_id")
    private Operador destinatarioOperador;

    public Notificacion() {
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

    public String getTipoCanal() {
        return tipoCanal;
    }

    public void setTipoCanal(String tipoCanal) {
        this.tipoCanal = tipoCanal;
    }

    public LocalDateTime getFechaEnvio() {
        return fechaEnvio;
    }

    public void setFechaEnvio(LocalDateTime fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }

    public EstadoNotificacion getEstadoEnvio() {
        return estadoEnvio;
    }

    public void setEstadoEnvio(EstadoNotificacion estadoEnvio) {
        this.estadoEnvio = estadoEnvio;
    }

    public int getIntentos() {
        return intentos == null ? 0 : intentos;
    }

    public void setIntentos(int intentos) {
        this.intentos = intentos;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }

    public LocalDateTime getFechaLectura() {
        return fechaLectura;
    }

    public void setFechaLectura(LocalDateTime fechaLectura) {
        this.fechaLectura = fechaLectura;
    }

    public HistorialEstado getHistorialEstado() {
        return historialEstado;
    }

    public void setHistorialEstado(HistorialEstado historialEstado) {
        this.historialEstado = historialEstado;
    }

    public UsuarioTicket getDestinatarioUsuario() {
        return destinatarioUsuario;
    }

    public void setDestinatarioUsuario(UsuarioTicket destinatarioUsuario) {
        this.destinatarioUsuario = destinatarioUsuario;
    }

    public Operador getDestinatarioOperador() {
        return destinatarioOperador;
    }

    public void setDestinatarioOperador(Operador destinatarioOperador) {
        this.destinatarioOperador = destinatarioOperador;
    }
}

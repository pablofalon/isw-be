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

@Entity
@Table(name = "historial_estados")
public class HistorialEstado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fechaCambio;

    @Column(length = 2000)
    private String observacion;

    @Column(length = 2000)
    private String motivoCambio;

    @Enumerated(EnumType.STRING)
    private TipoRegistroHistorial tipo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_estado_id", nullable = false)
    private TicketEstado ticketEstado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_usuario_id")
    private UsuarioTicket actorUsuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_operador_id")
    private Operador actorOperador;

    public HistorialEstado() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getFechaCambio() {
        return fechaCambio;
    }

    public void setFechaCambio(LocalDateTime fechaCambio) {
        this.fechaCambio = fechaCambio;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public String getMotivoCambio() {
        return motivoCambio;
    }

    public void setMotivoCambio(String motivoCambio) {
        this.motivoCambio = motivoCambio;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }

    public TicketEstado getTicketEstado() {
        return ticketEstado;
    }

    public void setTicketEstado(TicketEstado ticketEstado) {
        this.ticketEstado = ticketEstado;
    }

    public UsuarioTicket getActorUsuario() {
        return actorUsuario;
    }

    public void setActorUsuario(UsuarioTicket actorUsuario) {
        this.actorUsuario = actorUsuario;
    }

    public Operador getActorOperador() {
        return actorOperador;
    }

    public void setActorOperador(Operador actorOperador) {
        this.actorOperador = actorOperador;
    }

    public TipoRegistroHistorial getTipo() {
        return tipo;
    }

    public void setTipo(TipoRegistroHistorial tipo) {
        this.tipo = tipo;
    }
}

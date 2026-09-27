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
@Table(name = "historial_asignaciones")
public class HistorialAsignacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fechaAsignacion;

    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoAccionAsignacion tipoAccion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operador_anterior_id")
    private Operador operadorAnterior;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operador_nuevo_id", nullable = false)
    private Operador operadorNuevo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operador_responsable_id", nullable = false)
    private Operador operadorResponsable;

    public HistorialAsignacion() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getFechaAsignacion() {
        return fechaAsignacion;
    }

    public void setFechaAsignacion(LocalDateTime fechaAsignacion) {
        this.fechaAsignacion = fechaAsignacion;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public TipoAccionAsignacion getTipoAccion() {
        return tipoAccion;
    }

    public void setTipoAccion(TipoAccionAsignacion tipoAccion) {
        this.tipoAccion = tipoAccion;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }

    public Operador getOperadorAnterior() {
        return operadorAnterior;
    }

    public void setOperadorAnterior(Operador operadorAnterior) {
        this.operadorAnterior = operadorAnterior;
    }

    public Operador getOperadorNuevo() {
        return operadorNuevo;
    }

    public void setOperadorNuevo(Operador operadorNuevo) {
        this.operadorNuevo = operadorNuevo;
    }

    public Operador getOperadorResponsable() {
        return operadorResponsable;
    }

    public void setOperadorResponsable(Operador operadorResponsable) {
        this.operadorResponsable = operadorResponsable;
    }
}

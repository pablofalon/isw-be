package org.proyecto.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "comentarios")
public class Comentario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2000)
    private String contenido;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(nullable = false)
    private boolean esInterno = false;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_autor_id")
    private UsuarioTicket usuarioAutor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operador_autor_id")
    private Operador operadorAutor;

    public Comentario() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public boolean isEsInterno() {
        return esInterno;
    }

    public void setEsInterno(boolean esInterno) {
        this.esInterno = esInterno;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }

    public UsuarioTicket getUsuarioAutor() {
        return usuarioAutor;
    }

    public void setUsuarioAutor(UsuarioTicket usuarioAutor) {
        this.usuarioAutor = usuarioAutor;
    }

    public Operador getOperadorAutor() {
        return operadorAutor;
    }

    public void setOperadorAutor(Operador operadorAutor) {
        this.operadorAutor = operadorAutor;
    }
}

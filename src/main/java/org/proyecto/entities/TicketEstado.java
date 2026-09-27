package org.proyecto.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Catálogo de estados del ticket (antes un enum simple; ver Hoja de Seguimiento del documento:
 * "Especificar el nombre de la clase estado a algo mas especifico. TicketEstado").
 */
@Entity
@Table(name = "ticket_estados")
public class TicketEstado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private TicketEstadoCodigo codigo;

    @Column(nullable = false)
    private String nombre;

    private String descripcion;

    @Column(nullable = false)
    private boolean estadoFinal;

    @Column(nullable = false)
    private boolean permiteReapertura;

    public TicketEstado() {
    }

    public TicketEstado(TicketEstadoCodigo codigo, String nombre, String descripcion,
                         boolean estadoFinal, boolean permiteReapertura) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.estadoFinal = estadoFinal;
        this.permiteReapertura = permiteReapertura;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TicketEstadoCodigo getCodigo() {
        return codigo;
    }

    public void setCodigo(TicketEstadoCodigo codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public boolean isEstadoFinal() {
        return estadoFinal;
    }

    public void setEstadoFinal(boolean estadoFinal) {
        this.estadoFinal = estadoFinal;
    }

    public boolean isPermiteReapertura() {
        return permiteReapertura;
    }

    public void setPermiteReapertura(boolean permiteReapertura) {
        this.permiteReapertura = permiteReapertura;
    }
}

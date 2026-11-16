package org.proyecto.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Registro de un cambio de parámetros (CU25 paso 7): fecha, administrador y valor anterior. */
@Entity
@Table(name = "auditoria_parametros")
public class AuditoriaParametro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false)
    private String administrador;

    @Column(nullable = false, length = 50)
    private String grupo;

    @Column(nullable = false)
    private String elemento;

    @Column(nullable = false, length = 50)
    private String accion;

    @Column(length = 500)
    private String valorAnterior;

    @Column(length = 500)
    private String valorNuevo;

    public AuditoriaParametro() {
    }

    public AuditoriaParametro(LocalDateTime fecha, String administrador, String grupo, String elemento,
                              String accion, String valorAnterior, String valorNuevo) {
        this.fecha = fecha;
        this.administrador = administrador;
        this.grupo = grupo;
        this.elemento = elemento;
        this.accion = accion;
        this.valorAnterior = valorAnterior;
        this.valorNuevo = valorNuevo;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public String getAdministrador() {
        return administrador;
    }

    public String getGrupo() {
        return grupo;
    }

    public String getElemento() {
        return elemento;
    }

    public String getAccion() {
        return accion;
    }

    public String getValorAnterior() {
        return valorAnterior;
    }

    public String getValorNuevo() {
        return valorNuevo;
    }
}

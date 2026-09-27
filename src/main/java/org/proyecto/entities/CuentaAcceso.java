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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Credenciales de acceso al sistema. Según la observación de modelado del CU23 del documento,
 * ni UsuarioTicket ni Operador tienen usuario/contraseña propios: se vinculan opcionalmente
 * (exactamente una de las dos referencias, según el rol) a esta clase.
 */
@Entity
@Table(name = "cuentas_acceso")
public class CuentaAcceso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String usuarioLogin;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol;

    @Column(nullable = false)
    private int intentosFallidos = 0;

    private LocalDateTime fechaUltimoAcceso;

    @Column(nullable = false)
    private boolean activa = true;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_ticket_id")
    private UsuarioTicket usuarioTicket;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operador_id")
    private Operador operador;

    public CuentaAcceso() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsuarioLogin() {
        return usuarioLogin;
    }

    public void setUsuarioLogin(String usuarioLogin) {
        this.usuarioLogin = usuarioLogin;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public int getIntentosFallidos() {
        return intentosFallidos;
    }

    public void setIntentosFallidos(int intentosFallidos) {
        this.intentosFallidos = intentosFallidos;
    }

    public LocalDateTime getFechaUltimoAcceso() {
        return fechaUltimoAcceso;
    }

    public void setFechaUltimoAcceso(LocalDateTime fechaUltimoAcceso) {
        this.fechaUltimoAcceso = fechaUltimoAcceso;
    }

    public boolean isActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }

    public UsuarioTicket getUsuarioTicket() {
        return usuarioTicket;
    }

    public void setUsuarioTicket(UsuarioTicket usuarioTicket) {
        this.usuarioTicket = usuarioTicket;
    }

    public Operador getOperador() {
        return operador;
    }

    public void setOperador(Operador operador) {
        this.operador = operador;
    }
}

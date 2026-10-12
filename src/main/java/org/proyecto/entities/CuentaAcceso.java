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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Credenciales de acceso al sistema. Según la observación de modelado del CU23 del documento,
 * ni UsuarioTicket ni Operador tienen usuario/contraseña propios: se vinculan opcionalmente
 * (exactamente una de las dos referencias, según el rol) a esta clase.
 *
 * La autorización se resuelve con los componentes de permiso asignados (Composite: grupos y/o
 * permisos sueltos). El rol queda solo como tipo de cuenta (a qué entidad se vincula) y para
 * mostrarse en pantalla; no se usa para autorizar.
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

    /** true mientras la contraseña sea temporal (alta o blanqueo): el primer ingreso exige cambiarla. */
    @Column(nullable = false)
    private boolean debeCambiarPassword = false;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_ticket_id")
    private UsuarioTicket usuarioTicket;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operador_id")
    private Operador operador;

    @ManyToMany
    @JoinTable(name = "cuenta_componentes",
            joinColumns = @JoinColumn(name = "cuenta_id"),
            inverseJoinColumns = @JoinColumn(name = "componente_id"))
    private List<ComponentePermiso> componentes = new ArrayList<>();

    public CuentaAcceso() {
    }

    /** Permisos efectivos de la cuenta: unión de lo que aporta cada componente asignado. */
    public Set<String> obtenerPermisos() {
        Set<String> permisos = new LinkedHashSet<>();
        for (ComponentePermiso componente : componentes) {
            permisos.addAll(componente.obtenerPermisos());
        }
        return permisos;
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

    public boolean isDebeCambiarPassword() {
        return debeCambiarPassword;
    }

    public void setDebeCambiarPassword(boolean debeCambiarPassword) {
        this.debeCambiarPassword = debeCambiarPassword;
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

    public List<ComponentePermiso> getComponentes() {
        return componentes;
    }

    public void setComponentes(List<ComponentePermiso> componentes) {
        this.componentes = componentes;
    }
}

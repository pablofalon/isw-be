package org.proyecto.presentation;

import org.proyecto.business.AuthService;
import org.proyecto.entities.ComponentePermiso;
import org.proyecto.entities.CuentaAcceso;
import org.proyecto.entities.Operador;
import org.proyecto.entities.Rol;
import org.proyecto.entities.UsuarioTicket;

import java.time.LocalDateTime;
import java.util.List;

/** Usuario visto desde el ABM: datos de la cuenta y de la persona vinculada (usuario u operador). */
public class UsuarioResponse {

    private Long id;
    private String usuarioLogin;
    private Rol tipo;
    private boolean activa;
    private boolean bloqueadaPorIntentos;
    private boolean debeCambiarPassword;
    private LocalDateTime fechaUltimoAcceso;
    private String nombre;
    private String apellido;
    private String correoElectronico;
    private String telefono;
    private String area;
    private String cargo;
    private List<String> grupos;

    public UsuarioResponse() {
    }

    public static UsuarioResponse from(CuentaAcceso cuenta) {
        UsuarioResponse response = new UsuarioResponse();
        response.id = cuenta.getId();
        response.usuarioLogin = cuenta.getUsuarioLogin();
        response.tipo = cuenta.getRol();
        response.activa = cuenta.isActiva();
        response.bloqueadaPorIntentos = !cuenta.isActiva() && cuenta.getIntentosFallidos() >= AuthService.MAXIMO_INTENTOS_FALLIDOS;
        response.debeCambiarPassword = cuenta.isDebeCambiarPassword();
        response.fechaUltimoAcceso = cuenta.getFechaUltimoAcceso();
        response.grupos = cuenta.getComponentes().stream().map(ComponentePermiso::getCodigo).toList();
        UsuarioTicket usuario = cuenta.getUsuarioTicket();
        Operador operador = cuenta.getOperador();
        if (usuario != null) {
            response.nombre = usuario.getNombre();
            response.apellido = usuario.getApellido();
            response.correoElectronico = usuario.getCorreoElectronico();
            response.telefono = usuario.getTelefono();
        } else if (operador != null) {
            response.nombre = operador.getNombre();
            response.apellido = operador.getApellido();
            response.correoElectronico = operador.getCorreoElectronico();
            response.area = operador.getArea();
            response.cargo = operador.getCargo();
        }
        return response;
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

    public Rol getTipo() {
        return tipo;
    }

    public void setTipo(Rol tipo) {
        this.tipo = tipo;
    }

    public boolean getActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }

    public boolean getBloqueadaPorIntentos() {
        return bloqueadaPorIntentos;
    }

    public void setBloqueadaPorIntentos(boolean bloqueadaPorIntentos) {
        this.bloqueadaPorIntentos = bloqueadaPorIntentos;
    }

    public boolean getDebeCambiarPassword() {
        return debeCambiarPassword;
    }

    public void setDebeCambiarPassword(boolean debeCambiarPassword) {
        this.debeCambiarPassword = debeCambiarPassword;
    }

    public LocalDateTime getFechaUltimoAcceso() {
        return fechaUltimoAcceso;
    }

    public void setFechaUltimoAcceso(LocalDateTime fechaUltimoAcceso) {
        this.fechaUltimoAcceso = fechaUltimoAcceso;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getCorreoElectronico() {
        return correoElectronico;
    }

    public void setCorreoElectronico(String correoElectronico) {
        this.correoElectronico = correoElectronico;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }

    public List<String> getGrupos() {
        return grupos;
    }

    public void setGrupos(List<String> grupos) {
        this.grupos = grupos;
    }
}

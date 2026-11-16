package org.proyecto.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Parámetro general del sistema (CU25): valor entero acotado a un rango, con valor por defecto. */
@Entity
@Table(name = "parametros_sistema")
public class ParametroSistema {

    public static final String UMBRAL_ALERTA_TIEMPOS = "umbral_alerta_tiempos";
    public static final String LOGIN_MAXIMO_INTENTOS = "login_maximo_intentos";
    public static final String LOGIN_MINUTOS_BLOQUEO = "login_minutos_bloqueo";

    @Id
    private String clave;

    @Column(nullable = false)
    private String descripcion;

    private String unidad;

    @Column(nullable = false)
    private int valor;

    @Column(nullable = false)
    private int valorDefecto;

    @Column(nullable = false)
    private int minimo;

    @Column(nullable = false)
    private int maximo;

    public ParametroSistema() {
    }

    public ParametroSistema(String clave, String descripcion, String unidad, int valor, int minimo, int maximo) {
        this.clave = clave;
        this.descripcion = descripcion;
        this.unidad = unidad;
        this.valor = valor;
        this.valorDefecto = valor;
        this.minimo = minimo;
        this.maximo = maximo;
    }

    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getUnidad() {
        return unidad;
    }

    public void setUnidad(String unidad) {
        this.unidad = unidad;
    }

    public int getValor() {
        return valor;
    }

    public void setValor(int valor) {
        this.valor = valor;
    }

    public int getValorDefecto() {
        return valorDefecto;
    }

    public void setValorDefecto(int valorDefecto) {
        this.valorDefecto = valorDefecto;
    }

    public int getMinimo() {
        return minimo;
    }

    public void setMinimo(int minimo) {
        this.minimo = minimo;
    }

    public int getMaximo() {
        return maximo;
    }

    public void setMaximo(int maximo) {
        this.maximo = maximo;
    }
}

package org.proyecto.entities;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

import java.util.List;
import java.util.Set;

/**
 * Componente del patrón Composite de permisos. Permiso es la hoja y GrupoPermisos el compuesto; el
 * cliente (Autorizador / PermisoService) trata a ambos de forma uniforme mediante
 * obtenerPermisos(). Es clase abstracta y no interfaz porque JPA solo mapea clases.
 *
 * Persistencia SINGLE_TABLE: una sola tabla con la columna discriminadora "tipo". Con dos subtipos
 * sin campos propios, JOINED solo agregaría joins, y la relación padre-hijo puede apuntar a
 * cualquier componente sin importar el subtipo.
 */
@Entity
@Table(name = "componentes_permiso")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo", discriminatorType = DiscriminatorType.STRING)
public abstract class ComponentePermiso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigo;

    private String descripcion;

    protected ComponentePermiso() {
    }

    protected ComponentePermiso(String codigo, String descripcion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
    }

    public abstract void agregar(ComponentePermiso componente);

    public abstract void quitar(ComponentePermiso componente);

    public abstract List<ComponentePermiso> getHijos();

    /** Códigos de todos los permisos alcanzables desde este componente (recursivo, sin duplicados). */
    public abstract Set<String> obtenerPermisos();

    /** true si este componente es el indicado o lo contiene en algún nivel; se usa para evitar ciclos. */
    public abstract boolean esOContiene(ComponentePermiso componente);

    public abstract String getTipo();

    protected boolean esMismo(ComponentePermiso otro) {
        return this == otro || (codigo != null && codigo.equals(otro.getCodigo()));
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}

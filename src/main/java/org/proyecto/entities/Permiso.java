package org.proyecto.entities;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.util.List;
import java.util.Set;

/** Hoja del Composite: un permiso atómico (ej. TICKET_CLASIFICAR). No admite hijos. */
@Entity
@DiscriminatorValue("PERMISO")
public class Permiso extends ComponentePermiso {

    public static final String TIPO = "PERMISO";

    protected Permiso() {
    }

    public Permiso(String codigo, String descripcion) {
        super(codigo, descripcion);
    }

    @Override
    public void agregar(ComponentePermiso componente) {
        throw new UnsupportedOperationException("Un permiso no puede tener hijos");
    }

    @Override
    public void quitar(ComponentePermiso componente) {
        throw new UnsupportedOperationException("Un permiso no puede tener hijos");
    }

    @Override
    public List<ComponentePermiso> getHijos() {
        return List.of();
    }

    @Override
    public Set<String> obtenerPermisos() {
        return Set.of(getCodigo());
    }

    @Override
    public boolean esOContiene(ComponentePermiso componente) {
        return esMismo(componente);
    }

    @Override
    public String getTipo() {
        return TIPO;
    }
}

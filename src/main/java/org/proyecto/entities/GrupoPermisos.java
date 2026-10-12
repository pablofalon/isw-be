package org.proyecto.entities;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Compuesto del Composite: agrupa permisos y otros grupos. La relación con los hijos es
 * @ManyToMany (y no @OneToMany) para que un mismo componente pueda reutilizarse dentro de más de
 * un grupo sin duplicarse (ej. GESTION_TICKETS dentro de OPERADOR y de ADMINISTRADOR). Sin
 * cascade: cada componente se persiste por su cuenta y el grupo solo referencia a sus hijos.
 */
@Entity
@DiscriminatorValue("GRUPO")
public class GrupoPermisos extends ComponentePermiso {

    public static final String TIPO = "GRUPO";

    @ManyToMany
    @JoinTable(name = "grupo_hijos",
            joinColumns = @JoinColumn(name = "grupo_id"),
            inverseJoinColumns = @JoinColumn(name = "hijo_id"))
    private List<ComponentePermiso> hijos = new ArrayList<>();

    protected GrupoPermisos() {
    }

    public GrupoPermisos(String codigo, String descripcion) {
        super(codigo, descripcion);
    }

    /**
     * Rechaza los ciclos: como un componente puede tener varios padres, nada impide armar
     * A -> B -> A, y obtenerPermisos() no terminaría nunca.
     */
    @Override
    public void agregar(ComponentePermiso componente) {
        if (componente.esOContiene(this)) {
            throw new ComposicionInvalidaException(
                    "No se puede agregar " + componente.getCodigo() + " a " + getCodigo() + ": se formaría un ciclo");
        }
        if (hijos.stream().anyMatch(hijo -> hijo.esMismo(componente))) {
            throw new ComposicionInvalidaException(
                    getCodigo() + " ya contiene a " + componente.getCodigo());
        }
        hijos.add(componente);
    }

    @Override
    public void quitar(ComponentePermiso componente) {
        if (!hijos.removeIf(hijo -> hijo.esMismo(componente))) {
            throw new ComposicionInvalidaException(
                    getCodigo() + " no contiene a " + componente.getCodigo());
        }
    }

    @Override
    public List<ComponentePermiso> getHijos() {
        return Collections.unmodifiableList(hijos);
    }

    @Override
    public Set<String> obtenerPermisos() {
        Set<String> permisos = new LinkedHashSet<>();
        for (ComponentePermiso hijo : hijos) {
            permisos.addAll(hijo.obtenerPermisos()); // recursión
        }
        return permisos;
    }

    @Override
    public boolean esOContiene(ComponentePermiso componente) {
        return esMismo(componente) || hijos.stream().anyMatch(hijo -> hijo.esOContiene(componente));
    }

    @Override
    public String getTipo() {
        return TIPO;
    }
}

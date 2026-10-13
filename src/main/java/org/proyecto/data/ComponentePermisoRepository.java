package org.proyecto.data;

import org.proyecto.entities.ComponentePermiso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ComponentePermisoRepository extends JpaRepository<ComponentePermiso, Long> {

    Optional<ComponentePermiso> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);
}

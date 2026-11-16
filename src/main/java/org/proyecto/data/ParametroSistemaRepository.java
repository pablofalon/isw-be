package org.proyecto.data;

import org.proyecto.entities.ParametroSistema;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParametroSistemaRepository extends JpaRepository<ParametroSistema, String> {

    List<ParametroSistema> findAllByOrderByClave();
}

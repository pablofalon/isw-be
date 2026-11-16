package org.proyecto.data;

import org.proyecto.entities.AuditoriaParametro;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditoriaParametroRepository extends JpaRepository<AuditoriaParametro, Long> {

    List<AuditoriaParametro> findAllByOrderByFechaDescIdDesc(Pageable pageable);
}

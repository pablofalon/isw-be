package org.proyecto.data;

import org.proyecto.entities.Operador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OperadorRepository extends JpaRepository<Operador, Long> {

    List<Operador> findByActivoTrue();

    List<Operador> findByAreaAndActivoTrue(String area);
}

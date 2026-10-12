package org.proyecto.data;

import org.proyecto.entities.Operador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OperadorRepository extends JpaRepository<Operador, Long> {

    List<Operador> findByActivoTrue();

    Optional<Operador> findByCorreoElectronico(String correoElectronico);

    List<Operador> findByAreaAndActivoTrue(String area);
}

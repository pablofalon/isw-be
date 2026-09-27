package org.proyecto.data;

import org.proyecto.entities.NivelPrioridad;
import org.proyecto.entities.Prioridad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PrioridadRepository extends JpaRepository<Prioridad, Long> {

    Optional<Prioridad> findByNivel(NivelPrioridad nivel);
}

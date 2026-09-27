package org.proyecto.data;

import org.proyecto.entities.TicketEstado;
import org.proyecto.entities.TicketEstadoCodigo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TicketEstadoRepository extends JpaRepository<TicketEstado, Long> {

    Optional<TicketEstado> findByCodigo(TicketEstadoCodigo codigo);
}

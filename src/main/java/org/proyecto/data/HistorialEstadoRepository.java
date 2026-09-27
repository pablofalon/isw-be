package org.proyecto.data;

import org.proyecto.entities.HistorialEstado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistorialEstadoRepository extends JpaRepository<HistorialEstado, Long> {

    List<HistorialEstado> findByTicketIdOrderByFechaCambioAsc(Long ticketId);
}

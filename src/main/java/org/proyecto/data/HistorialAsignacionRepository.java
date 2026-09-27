package org.proyecto.data;

import org.proyecto.entities.HistorialAsignacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistorialAsignacionRepository extends JpaRepository<HistorialAsignacion, Long> {

    List<HistorialAsignacion> findByTicketIdOrderByFechaAsignacionAsc(Long ticketId);
}

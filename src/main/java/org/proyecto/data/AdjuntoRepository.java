package org.proyecto.data;

import org.proyecto.entities.Adjunto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdjuntoRepository extends JpaRepository<Adjunto, Long> {

    List<Adjunto> findByTicketIdOrderByFechaCargaAsc(Long ticketId);

    long countByTicketId(Long ticketId);
}

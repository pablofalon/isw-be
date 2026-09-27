package org.proyecto.data;

import org.proyecto.entities.Ticket;
import org.proyecto.entities.TicketEstadoCodigo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByUsuarioIdOrderByFechaCreacionDesc(Long usuarioId);

    List<Ticket> findByOperadorIdOrderByFechaCreacionDesc(Long operadorId);

    List<Ticket> findByEstadoActual_CodigoInOrderByFechaCreacionAsc(List<TicketEstadoCodigo> codigos);

    List<Ticket> findByEstadoActual_CodigoAndOperadorIsNullOrderByFechaCreacionAsc(TicketEstadoCodigo codigo);
}

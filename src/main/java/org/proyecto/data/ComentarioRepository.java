package org.proyecto.data;

import org.proyecto.entities.Comentario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComentarioRepository extends JpaRepository<Comentario, Long> {

    List<Comentario> findByTicketIdOrderByFechaCreacionAsc(Long ticketId);
}

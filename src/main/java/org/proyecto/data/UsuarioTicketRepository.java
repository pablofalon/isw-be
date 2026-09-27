package org.proyecto.data;

import org.proyecto.entities.UsuarioTicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioTicketRepository extends JpaRepository<UsuarioTicket, Long> {

    Optional<UsuarioTicket> findByCorreoElectronico(String correoElectronico);
}

package org.proyecto.data;

import org.proyecto.entities.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findByDestinatarioUsuarioIdOrderByFechaEnvioDesc(Long usuarioId);

    List<Notificacion> findByDestinatarioOperadorIdOrderByFechaEnvioDesc(Long operadorId);
}

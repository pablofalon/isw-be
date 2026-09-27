package org.proyecto.data;

import org.proyecto.entities.CuentaAcceso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CuentaAccesoRepository extends JpaRepository<CuentaAcceso, Long> {

    Optional<CuentaAcceso> findByUsuarioLogin(String usuarioLogin);
}

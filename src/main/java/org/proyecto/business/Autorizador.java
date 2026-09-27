package org.proyecto.business;

import org.proyecto.entities.CuentaAcceso;
import org.proyecto.entities.Rol;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Chequeo de rol simple (no reemplaza el mecanismo de autenticación existente, solo lo
 * complementa). No hay Spring Security en este proyecto: la autenticación se resuelve vía
 * SessionManager + AuthenticatedSessionArgumentResolver, y este componente valida autorización
 * (RF23/CU23 exige habilitar funcionalidades "según su perfil").
 */
@Component
public class Autorizador {

    public void requireRol(CuentaAcceso cuenta, Rol... rolesPermitidos) {
        Set<Rol> permitidos = Set.of(rolesPermitidos);
        if (!permitidos.contains(cuenta.getRol())) {
            throw new ForbiddenOperationException(
                    "La cuenta no tiene el rol requerido para esta operación: " + permitidos);
        }
    }
}

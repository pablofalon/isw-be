package org.proyecto.business;

import org.springframework.stereotype.Component;

/**
 * Chequeo de autorización por permiso (no reemplaza el mecanismo de autenticación existente, solo
 * lo complementa). No hay Spring Security en este proyecto: la autenticación se resuelve vía
 * SessionManager + AuthenticatedSessionArgumentResolver, y este componente valida que la sesión
 * tenga el permiso pedido, venga asignado directamente o a través de cualquier nivel de grupos
 * (RF23/CU23 exige habilitar funcionalidades "según su perfil").
 */
@Component
public class Autorizador {

    public void requirePermiso(SesionActiva sesion, String permiso) {
        if (!sesion.tiene(permiso)) {
            throw new ForbiddenOperationException("La cuenta no tiene el permiso requerido para esta operación: " + permiso);
        }
    }
}

package org.proyecto.business;

import org.junit.jupiter.api.Test;
import org.proyecto.entities.CuentaAcceso;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionManagerTest {

    private final SessionManager sessionManager = SessionManager.getInstance();

    @Test
    void esSingleton() {
        assertSame(sessionManager, SessionManager.getInstance());
    }

    @Test
    void guardaLaCuentaConSusPermisosYLaInvalida() {
        CuentaAcceso cuenta = new CuentaAcceso();
        cuenta.setId(1L);

        String token = sessionManager.createSession(cuenta, Set.of(Permisos.TICKET_CREAR));
        SesionActiva sesion = sessionManager.getSession(token).orElseThrow();

        assertSame(cuenta, sesion.cuenta());
        assertTrue(sesion.tiene(Permisos.TICKET_CREAR));

        sessionManager.invalidateSession(token);
        assertTrue(sessionManager.getSession(token).isEmpty());
    }

    @Test
    void recalcularPermisosActualizaLasSesionesAbiertas() {
        CuentaAcceso cuenta = new CuentaAcceso();
        cuenta.setId(2L);
        String token = sessionManager.createSession(cuenta, Set.of(Permisos.TICKET_CREAR));

        sessionManager.recalcularPermisos(id -> id.equals(2L) ? Set.of(Permisos.TICKET_RESOLVER) : Set.of());

        assertEquals(Set.of(Permisos.TICKET_RESOLVER), sessionManager.getSession(token).orElseThrow().permisos());
        sessionManager.invalidateSession(token);
    }
}

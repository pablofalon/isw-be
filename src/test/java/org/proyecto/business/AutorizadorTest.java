package org.proyecto.business;

import org.junit.jupiter.api.Test;
import org.proyecto.entities.CuentaAcceso;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AutorizadorTest {

    private final Autorizador autorizador = new Autorizador();

    @Test
    void conElPermisoNoLanza() {
        SesionActiva sesion = new SesionActiva(new CuentaAcceso(), Set.of(Permisos.TICKET_CLASIFICAR));

        assertDoesNotThrow(() -> autorizador.requirePermiso(sesion, Permisos.TICKET_CLASIFICAR));
    }

    @Test
    void sinElPermisoLanzaForbidden() {
        SesionActiva sesion = new SesionActiva(new CuentaAcceso(), Set.of(Permisos.TICKET_CREAR));

        assertThrows(ForbiddenOperationException.class,
                () -> autorizador.requirePermiso(sesion, Permisos.TICKET_CLASIFICAR));
    }
}

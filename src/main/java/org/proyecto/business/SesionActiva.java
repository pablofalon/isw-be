package org.proyecto.business;

import org.proyecto.entities.CuentaAcceso;

import java.util.Set;

/**
 * Lo que guarda el SessionManager por token: la cuenta, sus permisos efectivos ya calculados y si
 * todavía usa una contraseña temporal. Los permisos se resuelven al iniciar sesión (dentro de una
 * transacción) porque la cuenta queda desconectada del EntityManager entre requests y recorrer el
 * árbol lazy fallaría; además evita recorrer el Composite en cada request.
 */
public record SesionActiva(CuentaAcceso cuenta, Set<String> permisos, boolean debeCambiarPassword) {

    public SesionActiva {
        permisos = Set.copyOf(permisos);
    }

    public SesionActiva(CuentaAcceso cuenta, Set<String> permisos) {
        this(cuenta, permisos, false);
    }

    public boolean tiene(String permiso) {
        return permisos.contains(permiso);
    }

    SesionActiva conPermisos(Set<String> nuevosPermisos) {
        return new SesionActiva(cuenta, nuevosPermisos, debeCambiarPassword);
    }

    SesionActiva conPasswordVigente() {
        return new SesionActiva(cuenta, permisos, false);
    }
}

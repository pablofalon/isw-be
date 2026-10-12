package org.proyecto.business;

import org.proyecto.entities.CuentaAcceso;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Se mantiene el mismo mecanismo de sesión del módulo de seguridad ya evaluado (Singleton en
 * memoria, tokens UUID sin TTL). Guarda una SesionActiva: la CuentaAcceso junto con sus permisos
 * efectivos, que es lo que consulta el Autorizador.
 */
public class SessionManager {

    private static volatile SessionManager instance;

    private final Map<String, SesionActiva> activeSessions = new ConcurrentHashMap<>();

    private SessionManager() {
    }

    public static SessionManager getInstance() {
        if (instance == null) {
            synchronized (SessionManager.class) {
                if (instance == null) {
                    instance = new SessionManager();
                }
            }
        }
        return instance;
    }

    public String createSession(CuentaAcceso cuenta, Set<String> permisos) {
        String token = UUID.randomUUID().toString();
        activeSessions.put(token, new SesionActiva(cuenta, permisos, cuenta.isDebeCambiarPassword()));
        return token;
    }

    public Optional<SesionActiva> getSession(String token) {
        return Optional.ofNullable(activeSessions.get(token));
    }

    public void invalidateSession(String token) {
        activeSessions.remove(token);
    }

    /**
     * Recalcula los permisos de las sesiones abiertas tras un cambio en el árbol de permisos o en
     * las asignaciones, para que el cambio aplique sin obligar a volver a iniciar sesión.
     */
    public void recalcularPermisos(Function<Long, Set<String>> permisosPorCuentaId) {
        activeSessions.replaceAll((token, sesion) ->
                sesion.conPermisos(permisosPorCuentaId.apply(sesion.cuenta().getId())));
    }

    /** Cierra todas las sesiones de una cuenta (baja, blanqueo de contraseña). */
    public void invalidarSesionesDe(Long cuentaId) {
        activeSessions.values().removeIf(sesion -> cuentaId.equals(sesion.cuenta().getId()));
    }

    /** Tras cambiar la contraseña temporal, las sesiones de esa cuenta dejan de estar restringidas. */
    public void marcarPasswordVigente(Long cuentaId) {
        activeSessions.replaceAll((token, sesion) ->
                cuentaId.equals(sesion.cuenta().getId()) ? sesion.conPasswordVigente() : sesion);
    }
}

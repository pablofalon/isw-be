package org.proyecto.business;

import org.proyecto.entities.CuentaAcceso;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Se mantiene el mismo mecanismo de sesión del módulo de seguridad ya evaluado (Singleton en
 * memoria, tokens UUID sin TTL). Ahora guarda la CuentaAcceso (que expone el rol) en lugar de la
 * antigua entidad User.
 */
public class SessionManager {

    private static volatile SessionManager instance;

    private final Map<String, CuentaAcceso> activeSessions = new ConcurrentHashMap<>();

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

    public String createSession(CuentaAcceso cuenta) {
        String token = UUID.randomUUID().toString();
        activeSessions.put(token, cuenta);
        return token;
    }

    public Optional<CuentaAcceso> getSession(String token) {
        return Optional.ofNullable(activeSessions.get(token));
    }

    public void invalidateSession(String token) {
        activeSessions.remove(token);
    }
}

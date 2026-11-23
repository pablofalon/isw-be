package org.proyecto.business;

import org.proyecto.entities.CuentaAcceso;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
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

    private static final Duration INACTIVIDAD_POR_DEFECTO = Duration.ofMinutes(30);

    private final Map<String, SesionActiva> activeSessions = new ConcurrentHashMap<>();
    /** Último uso de cada token: la sesión expira tras un período de inactividad. */
    private final Map<String, Instant> ultimoAcceso = new ConcurrentHashMap<>();
    private volatile Duration tiempoInactividad = INACTIVIDAD_POR_DEFECTO;
    private volatile Clock reloj = Clock.systemUTC();

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
        ultimoAcceso.put(token, reloj.instant());
        return token;
    }

    /** Devuelve la sesión vigente y renueva su inactividad; una sesión vencida se elimina y no se devuelve. */
    public Optional<SesionActiva> getSession(String token) {
        SesionActiva sesion = activeSessions.get(token);
        if (sesion == null) {
            return Optional.empty();
        }
        Instant ahora = reloj.instant();
        Instant ultimo = ultimoAcceso.get(token);
        if (ultimo == null || Duration.between(ultimo, ahora).compareTo(tiempoInactividad) > 0) {
            invalidateSession(token);
            return Optional.empty();
        }
        ultimoAcceso.put(token, ahora);
        return Optional.of(sesion);
    }

    public void invalidateSession(String token) {
        activeSessions.remove(token);
        ultimoAcceso.remove(token);
    }

    /** Minutos de inactividad tras los cuales la sesión expira (app.sesion.minutos-inactividad). */
    public void setMinutosInactividad(long minutos) {
        this.tiempoInactividad = Duration.ofMinutes(minutos);
    }

    /** Solo para tests: reloj controlable. */
    void setReloj(Clock reloj) {
        this.reloj = reloj;
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
        activeSessions.entrySet().removeIf(entrada -> {
            boolean propia = cuentaId.equals(entrada.getValue().cuenta().getId());
            if (propia) {
                ultimoAcceso.remove(entrada.getKey());
            }
            return propia;
        });
    }

    /** Tras cambiar la contraseña temporal, las sesiones de esa cuenta dejan de estar restringidas. */
    public void marcarPasswordVigente(Long cuentaId) {
        activeSessions.replaceAll((token, sesion) ->
                cuentaId.equals(sesion.cuenta().getId()) ? sesion.conPasswordVigente() : sesion);
    }
}

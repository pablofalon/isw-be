package org.proyecto.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Reemplaza a la antigua @AuthenticatedUser: se usa como parámetro de controller para inyectar
 * la sesión autenticada (token Bearer -> SessionManager): SesionActiva cuando el endpoint necesita
 * autorizar por permiso, o directamente la CuentaAcceso cuando solo exige estar autenticado. Sigue siendo opt-in por endpoint,
 * tal como en el módulo de seguridad ya evaluado.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface AuthenticatedSession {

    /**
     * Con contraseña temporal la sesión solo sirve para cambiarla: el resto de los endpoints responde
     * 403 hasta que se cambie. Solo el endpoint de cambio de contraseña lo habilita.
     */
    boolean permitePasswordTemporal() default false;
}

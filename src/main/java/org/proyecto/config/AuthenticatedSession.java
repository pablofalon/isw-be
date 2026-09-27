package org.proyecto.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Reemplaza a la antigua @AuthenticatedUser: se usa como parámetro de controller para inyectar
 * la CuentaAcceso autenticada (token Bearer -> SessionManager). Sigue siendo opt-in por endpoint,
 * tal como en el módulo de seguridad ya evaluado.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface AuthenticatedSession {
}

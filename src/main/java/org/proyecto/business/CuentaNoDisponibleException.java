package org.proyecto.business;

/** CU23 FA2 / E1: credenciales correctas pero la cuenta está inactiva o bloqueada temporalmente. */
public class CuentaNoDisponibleException extends RuntimeException {

    public CuentaNoDisponibleException(String message) {
        super(message);
    }
}

package org.proyecto.business;

/** Operación del ABM de seguridad que no puede aplicarse con los datos recibidos (400). */
public class OperacionSeguridadInvalidaException extends RuntimeException {

    public OperacionSeguridadInvalidaException(String message) {
        super(message);
    }
}

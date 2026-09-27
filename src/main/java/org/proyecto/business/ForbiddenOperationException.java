package org.proyecto.business;

/** Se lanza cuando la cuenta autenticada no tiene el rol necesario para una operación (403). */
public class ForbiddenOperationException extends RuntimeException {

    public ForbiddenOperationException(String message) {
        super(message);
    }
}

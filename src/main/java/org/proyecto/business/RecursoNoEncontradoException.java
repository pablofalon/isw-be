package org.proyecto.business;

/** Cuenta o componente de permiso inexistente (404). */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String message) {
        super(message);
    }
}

package org.proyecto.entities;

/** Operación inválida sobre el árbol de permisos (ciclo, hijo duplicado o inexistente). */
public class ComposicionInvalidaException extends RuntimeException {

    public ComposicionInvalidaException(String message) {
        super(message);
    }
}

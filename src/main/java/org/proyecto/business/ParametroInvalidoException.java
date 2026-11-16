package org.proyecto.business;

/** Dato inválido al configurar parámetros (CU25): rango fuera de límites o nombre duplicado. 400. */
public class ParametroInvalidoException extends RuntimeException {

    public ParametroInvalidoException(String message) {
        super(message);
    }
}

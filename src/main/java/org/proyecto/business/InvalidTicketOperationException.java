package org.proyecto.business;

public class InvalidTicketOperationException extends RuntimeException {

    public InvalidTicketOperationException(String message) {
        super(message);
    }
}

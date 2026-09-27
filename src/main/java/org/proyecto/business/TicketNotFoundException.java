package org.proyecto.business;

public class TicketNotFoundException extends RuntimeException {

    public TicketNotFoundException(Long ticketId) {
        super("Ticket not found: " + ticketId);
    }
}

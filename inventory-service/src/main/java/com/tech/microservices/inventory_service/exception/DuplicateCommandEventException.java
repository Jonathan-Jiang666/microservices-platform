package com.tech.microservices.inventory_service.exception;

public class DuplicateCommandEventException extends RuntimeException{
    public DuplicateCommandEventException(java.util.UUID eventId, Throwable exception) {
        super("Event with ID " + eventId + " is already processed", exception);
    }
}

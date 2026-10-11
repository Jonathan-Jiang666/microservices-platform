package com.tech.microservices.inventory_service.exception;

public class InvalidReservationStatusException extends RuntimeException{
    public InvalidReservationStatusException(String message) {
        super(message);
    }

}

package com.tech.microservices.order_service.exception;

// 404
public class ReservationNotFoundException extends RuntimeException{
    public ReservationNotFoundException(String message) {
        super(message);
    }
}

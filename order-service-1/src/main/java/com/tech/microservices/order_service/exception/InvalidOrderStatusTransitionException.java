package com.tech.microservices.order_service.exception;

// 409 Conflict
public class InvalidOrderStatusTransitionException extends RuntimeException{
    public InvalidOrderStatusTransitionException(String message) {
        super(message);
    }
}

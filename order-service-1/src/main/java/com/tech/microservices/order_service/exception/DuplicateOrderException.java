package com.tech.microservices.order_service.exception;

public class DuplicateOrderException extends RuntimeException{
    public DuplicateOrderException(String message) {
        super(message);
    }
}

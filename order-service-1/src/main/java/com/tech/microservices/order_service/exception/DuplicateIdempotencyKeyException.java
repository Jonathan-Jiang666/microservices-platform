package com.tech.microservices.order_service.exception;

//409 Conflict
public class DuplicateIdempotencyKeyException extends RuntimeException{
    public DuplicateIdempotencyKeyException(String message) {
        super(message);
    }
}

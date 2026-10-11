package com.tech.microservices.inventory_service.exception;

public class IdempotencyConflictException extends RuntimeException{
    public IdempotencyConflictException(String idempotencyKey) {
        super("Idempotency key conflict: " + idempotencyKey);
    }
}

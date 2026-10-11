package com.tech.microservices.inventory_service.exception;

public class DuplicateIdempotencyKeyException extends RuntimeException{
    public DuplicateIdempotencyKeyException(String idempotencyKey) {
        super("Idempotency key already exists: " + idempotencyKey);
    }
}

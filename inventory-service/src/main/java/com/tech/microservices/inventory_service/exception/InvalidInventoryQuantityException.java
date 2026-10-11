package com.tech.microservices.inventory_service.exception;

public class InvalidInventoryQuantityException extends RuntimeException{
    public InvalidInventoryQuantityException(String message) {
        super(message);
    }
}

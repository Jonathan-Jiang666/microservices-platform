package com.tech.microservices.inventory_service.exception;

// 可用库存不足
public class InsufficientInventoryException extends RuntimeException{
    public InsufficientInventoryException(String message) {
        super(message);
    }
}

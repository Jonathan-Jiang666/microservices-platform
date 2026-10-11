package com.tech.microservices.inventory_service.exception;

public class DuplicateInventoryException extends RuntimeException{
    public DuplicateInventoryException(String skuId) {
        super("Inventory already exists for skuId:" + skuId);
    }
}

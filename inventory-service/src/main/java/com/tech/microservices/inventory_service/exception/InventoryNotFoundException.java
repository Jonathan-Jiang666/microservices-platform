package com.tech.microservices.inventory_service.exception;

public class InventoryNotFoundException extends RuntimeException{

    public InventoryNotFoundException(String skuId) {
        super("Inventory not found for skuId:" + skuId);

    }
}

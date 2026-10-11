package com.tech.microservices.order_service.integration.inventory.command;

public record InventoryReleaseRequestItem(
     String skuId,
     Long quantity
) {
    
}

package com.tech.microservices.inventory_service.dto.command;

public record InventoryReleaseRequestedItem(
    String skuId,
    Long quantity) {
    
}

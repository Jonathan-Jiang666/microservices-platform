package com.tech.microservices.inventory_service.dto.command;

public record InventoryReservationRequestedItem(
       String skuId,
       Long quantity
) {
    
}

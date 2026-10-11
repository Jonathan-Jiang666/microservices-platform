package com.tech.microservices.order_service.integration.inventory.command;

public record InventoryReservationRequestItem( 
        String skuId,
        Long quantity
    ) {
    
}

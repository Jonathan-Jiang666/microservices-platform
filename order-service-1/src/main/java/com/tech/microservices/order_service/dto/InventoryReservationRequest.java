package com.tech.microservices.order_service.dto;

public record InventoryReservationRequest( 
        String orderNo,
        String skuId,
        Long reservedQuantity) {
    
}

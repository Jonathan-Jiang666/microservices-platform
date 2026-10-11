package com.tech.microservices.order_service.dto;

public record InventoryReservationExpiredRequest(
        String orderNo,
        String skuId,
        Long expireQuantity
) {
    
}

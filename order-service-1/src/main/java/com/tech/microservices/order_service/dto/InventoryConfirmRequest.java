package com.tech.microservices.order_service.dto;

public record InventoryConfirmRequest(
        String orderNo,
        String skuId,
        Long confirmQuantity
) {
    
}

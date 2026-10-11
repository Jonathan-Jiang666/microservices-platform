package com.tech.microservices.order_service.dto;

public record InventoryReleaseRequest(
    String orderNo,
        String skuId,
        Long releaseQuantity
) {
    
}

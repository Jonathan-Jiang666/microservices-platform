package com.tech.microservices.order_service.dto;

public record InventoryExpireRequest(
       String orderNo,
        String skuId,
        Long expireQuantity
) {
    
}

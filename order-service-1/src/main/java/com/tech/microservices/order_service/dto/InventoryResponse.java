package com.tech.microservices.order_service.dto;

import java.time.LocalDateTime;

public record InventoryResponse(
        String skuId,
        Long onHandQuantity,
        Long reservedQuantity,
        Long availableQuantity,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

package com.tech.microservices.inventory_service.dto.event.item;

import java.time.LocalDateTime;

public record StockInEvent(
        Long inventoryId,
        String skuId,
        String eventType,
        Long quantity,
        Long quantityBefore,
        Long quantityAfter,
        LocalDateTime createdAt
) {
}

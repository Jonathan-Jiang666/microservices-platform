package com.tech.microservices.inventory_service.dto.event.item;

import java.time.LocalDateTime;

public record InventoryCreatedEvent(
        Long inventoryId,
        String eventType,
        String skuId,
        Long onHandQuantity,
        Long reservedQuantity,
        LocalDateTime createdAt
) {
}

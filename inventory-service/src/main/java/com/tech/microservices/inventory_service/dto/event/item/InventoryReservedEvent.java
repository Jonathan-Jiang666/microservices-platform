package com.tech.microservices.inventory_service.dto.event.item;

import java.time.LocalDateTime;

public record InventoryReservedEvent(

        Long inventoryId,
        String skuId,
        String orderNo,
        String reservationNo,
        String transactionNo,
        String eventType,
        Long reservedQuantity,
        Long reservedQuantityBefore,
        Long reservedQuantityAfter,
        Long availableQuantityBefore,
        Long availableQuantityAfter,
        LocalDateTime expiresAt,
        LocalDateTime createAt
) {
}

package com.tech.microservices.inventory_service.dto.event.item;

import java.time.LocalDateTime;

public record InventoryReservationExpiredEvent(

     Long inventoryId,
        String skuId,
        String orderNo,
        String reservationNo,
        String transactionNo,
        String eventType,
        Long expiredQuantity,
        Long reservedBefore,
        Long reservedAfter,
        Long availableBefore,
        Long availableAfter,
        LocalDateTime createdAt
)
 {
}

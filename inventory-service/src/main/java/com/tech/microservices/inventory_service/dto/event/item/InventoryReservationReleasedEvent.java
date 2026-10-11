package com.tech.microservices.inventory_service.dto.event.item;
import java.time.LocalDateTime;

public record InventoryReservationReleasedEvent(

        Long inventoryId,
        String orderNo,
        String skuId,
        String transactionNo,
        String reservationNo,
        String eventType,
        Long releaseQuantity,
        Long reservedQuantityBefore,
        Long reservedQuantityAfter,
        Long availableQuantityBefore,
        Long availableQuantityAfter,
        LocalDateTime releasedAt

) {
}

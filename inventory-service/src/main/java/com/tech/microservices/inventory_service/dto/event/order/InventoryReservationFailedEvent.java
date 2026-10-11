package com.tech.microservices.inventory_service.dto.event.order;

import java.time.LocalDateTime;
import java.util.UUID;

public record InventoryReservationFailedEvent(
        UUID commandEventId,
        String orderNumber,
        String failedSkuId,
        String reasonCode,
        String message,
        LocalDateTime failedAt
) {
    
}

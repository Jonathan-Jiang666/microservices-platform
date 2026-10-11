package com.tech.microservices.order_service.integration.inventory.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record InventoryReservationFailedEvent(
        UUID commandEventId,
        String failedSkuId,
        String orderNumber,
        String reasonCode,
        String message,
        LocalDateTime failedAt
        
) {
    
}

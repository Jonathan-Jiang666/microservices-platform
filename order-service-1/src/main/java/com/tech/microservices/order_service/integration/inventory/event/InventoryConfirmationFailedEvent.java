package com.tech.microservices.order_service.integration.inventory.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record InventoryConfirmationFailedEvent(
    UUID commandEventId,
        String orderNumber,
        String failedSkuId,
        String reasonCode,
        String message,
        LocalDateTime failedAt
) {
    
}

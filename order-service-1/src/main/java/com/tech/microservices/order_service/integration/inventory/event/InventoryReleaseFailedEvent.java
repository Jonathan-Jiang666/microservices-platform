package com.tech.microservices.order_service.integration.inventory.event;

import java.time.LocalDateTime;
import java.util.UUID;

import com.tech.microservices.order_service.integration.inventory.command.InventoryReleaseReason;

public record InventoryReleaseFailedEvent(
        UUID commandEventId,
        String orderNumber,
        InventoryReleaseReason releaseReason,
        String failedSkuId,
        String reasonCode,
        String message,
        LocalDateTime failedAt
) {
    
}

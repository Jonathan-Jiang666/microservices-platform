package com.tech.microservices.inventory_service.dto.event.order;

import java.time.LocalDateTime;
import java.util.UUID;

import com.tech.microservices.inventory_service.dto.command.InventoryReleaseReason;

public record InventoryReleaseFailedEvent(
     UUID commandEventId,
        String orderNumber,
        InventoryReleaseReason reason,
        String failedSkuId,
        String reasonCode,
        String message,
        LocalDateTime failedAt
) {
    
}

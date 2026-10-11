package com.tech.microservices.order_service.integration.inventory.event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.tech.microservices.order_service.integration.inventory.command.InventoryReleaseReason;
import com.tech.microservices.order_service.integration.inventory.command.InventoryReleaseRequestItem;

public record InventoryReleaseCompletedEvent(
    UUID commandEventId,
        String orderNumber,
        InventoryReleaseReason releaseReason,
        List<InventoryReleaseRequestItem> items,
        LocalDateTime completedAt
) {
    
}

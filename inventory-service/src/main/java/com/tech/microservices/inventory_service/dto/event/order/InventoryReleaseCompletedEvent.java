package com.tech.microservices.inventory_service.dto.event.order;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.tech.microservices.inventory_service.dto.command.InventoryReleaseReason;
import com.tech.microservices.inventory_service.dto.command.InventoryReleaseRequestedItem;

public record InventoryReleaseCompletedEvent(
    UUID commandEventId,
        String orderNumber,
        InventoryReleaseReason reason,
        List<InventoryReleaseRequestedItem> items,
        LocalDateTime completedAt
) {
    
}

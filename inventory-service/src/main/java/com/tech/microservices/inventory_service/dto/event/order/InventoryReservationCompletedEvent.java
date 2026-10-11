package com.tech.microservices.inventory_service.dto.event.order;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.tech.microservices.inventory_service.dto.command.InventoryReservationRequestedItem;

public record InventoryReservationCompletedEvent(
    UUID commandEventId,
        String orderNumber,
        List<InventoryReservationRequestedItem> items,
        LocalDateTime completedAt
) {
    
}

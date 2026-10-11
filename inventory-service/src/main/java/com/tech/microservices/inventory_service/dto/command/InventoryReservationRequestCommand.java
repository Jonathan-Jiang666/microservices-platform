package com.tech.microservices.inventory_service.dto.command;

import java.util.List;

public record InventoryReservationRequestCommand(
        String orderNumber,
        List<InventoryReservationRequestedItem> items
) {
    
}

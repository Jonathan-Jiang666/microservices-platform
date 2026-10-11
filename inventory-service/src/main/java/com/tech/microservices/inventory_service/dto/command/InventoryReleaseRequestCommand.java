 package com.tech.microservices.inventory_service.dto.command;

import java.util.List;

public record InventoryReleaseRequestCommand(
        String orderNumber,
        InventoryReleaseReason reason,
        List<InventoryReleaseRequestedItem> items
) {
}
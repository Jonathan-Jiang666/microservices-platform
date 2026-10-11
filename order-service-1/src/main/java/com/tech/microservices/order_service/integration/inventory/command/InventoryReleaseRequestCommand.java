package com.tech.microservices.order_service.integration.inventory.command;

import java.util.List;

public record InventoryReleaseRequestCommand(
    String orderNumber,
    InventoryReleaseReason reason,
     List<InventoryReleaseRequestItem> items
) {
    
}

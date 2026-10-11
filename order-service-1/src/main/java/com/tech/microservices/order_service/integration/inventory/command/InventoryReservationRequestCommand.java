package com.tech.microservices.order_service.integration.inventory.command;
import java.util.List;

public record InventoryReservationRequestCommand(
    String orderNumber,
     List<InventoryReservationRequestItem> items
) {
    
}

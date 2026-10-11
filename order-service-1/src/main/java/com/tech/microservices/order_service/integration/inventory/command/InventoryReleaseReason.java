package com.tech.microservices.order_service.integration.inventory.command;

public enum InventoryReleaseReason {
    ORDER_CANCELLATION,
    ORDER_EXPIRATION,
    ORDER_FAILURE
}

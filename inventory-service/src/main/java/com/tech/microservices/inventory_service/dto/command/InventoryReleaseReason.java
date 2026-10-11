package com.tech.microservices.inventory_service.dto.command;

public enum InventoryReleaseReason {
    ORDER_CANCELLATION,
    ORDER_EXPIRATION,
    ORDER_FAILURE
}

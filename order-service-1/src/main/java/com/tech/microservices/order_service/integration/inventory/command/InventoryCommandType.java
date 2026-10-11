package com.tech.microservices.order_service.integration.inventory.command;

public enum InventoryCommandType {
    // 库存相关命令 inventory reserve command
    INVENTORY_RESERVATION_REQUESTED,

    // 释放库存 inventory release command
    INVENTORY_RELEASE_REQUESTED,

    // 确认库存 inventory confirmation command
    INVENTORY_CONFIRMATION_REQUESTED,
    
    // 库存过期 inventory expiration command
    INVENTORY_EXPIRATION_REQUESTED
}

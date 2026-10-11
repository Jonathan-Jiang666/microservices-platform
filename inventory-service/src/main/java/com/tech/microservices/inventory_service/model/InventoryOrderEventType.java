package com.tech.microservices.inventory_service.model;

//表示一项订单级库存命令的执行结果 Order-level inventory command execution results
public enum InventoryOrderEventType {
    
    // 库存预定完成 inventory reservation completed
    INVENTORY_RESERVATION_COMPLETED,
    // 库存预定失败 inventory reservation failed
    INVENTORY_RESERVATION_FAILED,

    // 释放库存完成 inventory release completed
    INVENTORY_RELEASE_COMPLETED,
    // 释放库存失败 inventory release failed
    INVENTORY_RELEASE_FAILED,

    // 确认库存完成 inventory confirmation completed
    INVENTORY_CONFIRMATION_COMPLETED,
    // 确认库存失败 inventory confirmation failed
    INVENTORY_CONFIRMATION_FAILED,

    // 库存过期完成 inventory expiration completed
    INVENTORY_EXPIRATION_COMPLETED,
    // 库存过期失败 inventory expiration failed
    INVENTORY_EXPIRATION_FAILED
}

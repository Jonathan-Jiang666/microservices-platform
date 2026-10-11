package com.tech.microservices.inventory_service.model;

/**
 * 库存命令类型
 */
public enum InventoryCommandType {
    // 创建订单后预定库存 Inventory reservation requested after order is created
    INVENTORY_RESERVATION_REQUESTED,

    // 取消订单、关闭订单时释放库存 cancel order, close order when releasing inventory
    INVENTORY_RELEASE_REQUESTED,

    // 支付成功后确认库存扣减 minus inventory after payment is successful
    INVENTORY_CONFIRMATION_REQUESTED,

    // 订单超时后使库存预定过期 in case of order timeout, expire the inventory reservation
    INVENTORY_EXPIRATION_REQUESTED
}

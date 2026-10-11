package com.tech.microservices.order_service.model;

public enum OrderEventType {


    // 创建 create order
    ORDER_CREATED,

    // 订单信息修改 change order info
    ORDER_STATUS_CHANGED,
    ORDER_ITEMS_CHANGED,
    ORDER_BASIC_INFO_CHANGED,

    // 订单预定库存结果 inventory result
    ORDER_INVENTORY_RESERVED,
    ORDER_INVENTORY_FAILED,


    // 支付阶段 payment stage
    ORDER_PAYMENT_PENDING,
    ORDER_PAID,
    ORDER_PAYMENT_FAILED,

    // 订单生命周期 order lifecycle
    ORDER_CANCELLATION_REQUESTED,
    ORDER_CANCELLED,
    ORDER_CANCELLATION_FAILED,

    // 其他订单状态
    ORDER_SHIPPED,
    ORDER_CONFIRMED,
    ORDER_CONFIRMATION_FAILED,
    ORDER_COMPLETED,
    ORDER_EXPIRED,
    ORDER_EXPIRATION_FAILED,
    ORDER_REFUNDED,

    // 发给 Inventory Service 的命令 sent to Inventory Service 单独摘出来放到一个独立的枚举里 
    // 因为一个是纯订单的事件，一个是库存相关的事件，
    // 所以需要将它们分开定义
    //INVENTORY_RESERVATION_REQUESTED
    
}

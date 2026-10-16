package com.tech.microservices.order_service.model;

public enum OrderStatus {

    PENDING,                 // 订单刚创建，异步流程处理中
    AWAITING_PAYMENT,        // 库存准备完成，等待支付
    PAID,                    // 支付成功
    PROCESSING,              // 已支付，正在履约
    SHIPPED,                 // 已发货
    DELIVERED,               // 已送达
    COMPLETED,               // 订单完成

    CANCELLATION_PENDING,    // 取消处理中
    CANCELLATION_FAILED,     // 取消失败，可重试或人工处理
    CANCELLED,               // 已取消

    EXPIRED,                 // 已过期
    REFUND_PENDING,          // 退款处理中
    REFUNDED,                // 已退款
    REFUND_FAILED,           // 退款失败
    FAILED                   // 创建流程最终失败
}

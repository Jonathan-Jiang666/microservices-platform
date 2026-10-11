package com.tech.microservices.order_service.model;

public enum IdempotencyOperation {
    CREATE_ORDER,
    UPDATE_ORDER_STATUS,
    CANCEL_ORDER,
    UPDATE_ORDER_ITEMS,
    UPDATE_ORDER_BASIC_INFO,
    CONFIRM_ORDER_PAYMENT,
    COMPLETE_ORDER,
    REFUND_ORDER,
    EXPIRY_ORDER
}

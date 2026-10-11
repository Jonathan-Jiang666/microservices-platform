package com.tech.microservices.inventory_service.model;

public enum IdempotencyOperation {

    CREATE_INVENTORY,
    RESERVE_INVENTORY,
    RELEASE_RESERVATION,
    CONFIRM_RESERVATION,
    STOCK_IN,
    ADJUST_INVENTORY,
    EXPIRE_RESERVATION

}

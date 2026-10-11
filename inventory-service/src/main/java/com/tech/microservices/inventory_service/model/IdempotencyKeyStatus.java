package com.tech.microservices.inventory_service.model;

public enum IdempotencyKeyStatus {
    PROCESSING,
    COMPLETED,
    FAILED
}

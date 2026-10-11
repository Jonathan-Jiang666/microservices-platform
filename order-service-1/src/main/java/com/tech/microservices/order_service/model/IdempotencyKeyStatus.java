package com.tech.microservices.order_service.model;

public enum IdempotencyKeyStatus {
    PROCESSING,
    COMPLETED,
    FAILED
}

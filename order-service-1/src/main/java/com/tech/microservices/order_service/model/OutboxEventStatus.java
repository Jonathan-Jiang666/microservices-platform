package com.tech.microservices.order_service.model;

public enum OutboxEventStatus {
    PENDING,
    PUBLISHED,
    FAILED
}

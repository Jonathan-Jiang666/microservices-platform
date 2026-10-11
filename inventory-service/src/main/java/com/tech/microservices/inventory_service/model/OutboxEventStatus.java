package com.tech.microservices.inventory_service.model;

public enum OutboxEventStatus {
    PENDING,
    PUBLISHED,
    FAILED
}

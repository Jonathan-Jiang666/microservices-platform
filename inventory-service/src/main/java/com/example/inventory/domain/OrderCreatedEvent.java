package com.example.inventory.domain;

public record OrderCreatedEvent(Long orderId, String sku, int quantity) {
}

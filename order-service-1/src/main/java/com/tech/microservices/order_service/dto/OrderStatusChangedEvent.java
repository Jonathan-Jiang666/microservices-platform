package com.tech.microservices.order_service.dto;

import com.tech.microservices.order_service.model.OrderStatus;

import java.time.LocalDateTime;

public record OrderStatusChangedEvent(

        Long orderId,
        String orderNumber,
        Long userId,
        OrderStatus previousStatus,
        OrderStatus newStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

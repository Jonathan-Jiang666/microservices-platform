package com.tech.microservices.order_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderItemsChangedEvent(
        String orderNumber,
        Long userId,
        BigDecimal totalAmount,
        String currency,
        List<OrderItemCreatedEvent> orderItems,
        LocalDateTime updateAt
) {
}

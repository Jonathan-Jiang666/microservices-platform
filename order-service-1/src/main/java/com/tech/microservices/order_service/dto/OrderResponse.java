package com.tech.microservices.order_service.dto;

import com.tech.microservices.order_service.model.OrderStatus;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String orderNumber,
        Long userId,
        OrderStatus orderStatus,
        BigDecimal totalAmount,
        String currency,
        String idempotency_key,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long version,
        String note,
        String shippingAddress,
        String billingAddress,
        String recipientName,
        String recipientEmail,
        String recipientPhone,
        List<OrderItemResponse> orderItems
        ) {
}

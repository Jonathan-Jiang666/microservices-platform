package com.tech.microservices.order_service.dto;

import com.tech.microservices.order_service.model.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderSummaryResponse(

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
        String recipientPhone
) {
}

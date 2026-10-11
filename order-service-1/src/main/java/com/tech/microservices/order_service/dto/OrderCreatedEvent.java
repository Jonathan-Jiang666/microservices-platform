package com.tech.microservices.order_service.dto;

import java.math.BigDecimal;
import java.util.List;

import com.tech.microservices.order_service.model.OrderStatus;

public record OrderCreatedEvent(
        Long orderId,
        String orderNumber,
        Long userId,
        BigDecimal totalAmount,
        String currency,
        String recipientName,
        String recipientEmail,
        String recipientPhone,
        String shippingAddress,
        String billingAddress,
        OrderStatus orderStatus,
        List<OrderItemCreatedEvent> orderItems
) {
}

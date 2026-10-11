package com.tech.microservices.order_service.dto;

import java.time.LocalDateTime;

public record OrderBasicInfoChangedEvent(
        String orderNumber,
        Long userId,
        String shippingAddress,
        String billingAddress,
        String note,
        String recipientName,
        String recipientEmail,
        String recipientPhone,
        LocalDateTime updatedAt
) {
}

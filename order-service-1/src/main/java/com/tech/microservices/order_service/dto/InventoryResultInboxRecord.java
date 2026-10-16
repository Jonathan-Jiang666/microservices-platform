package com.tech.microservices.order_service.dto;

import java.time.LocalDateTime;

public record InventoryResultInboxRecord(
        String eventId,
        String eventType,
        String orderNumber,
        String payloadHash,
        LocalDateTime createAt
) {
}

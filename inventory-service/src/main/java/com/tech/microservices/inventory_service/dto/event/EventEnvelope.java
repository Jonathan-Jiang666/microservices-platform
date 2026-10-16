package com.tech.microservices.inventory_service.dto.event;

import java.time.LocalDateTime;
import java.util.UUID;

// 范型记录类
public record EventEnvelope<T>(
        UUID eventId,
        String eventType,
        Integer eventVersion,
        String aggregateType,
        String aggregateId,
        LocalDateTime occurredAt,
        T data
) {
    
}


package com.tech.microservices.inventory_service.exception;

import java.time.LocalDateTime;

public record ApiErrorResponse(
        int status,
        String code,
        String message,
        LocalDateTime timestamp
) {
}

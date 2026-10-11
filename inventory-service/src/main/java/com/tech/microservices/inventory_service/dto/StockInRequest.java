package com.tech.microservices.inventory_service.dto;

import jakarta.validation.constraints.*;

public record StockInRequest(
        @NotBlank
        @Size(max = 64)
        String skuId,

        @NotNull
        @Positive
        Long quantity
) {
}

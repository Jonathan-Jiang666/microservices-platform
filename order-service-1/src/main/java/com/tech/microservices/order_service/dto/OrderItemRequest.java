package com.tech.microservices.order_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record OrderItemRequest(

        @NotBlank
        String skuId,

        @NotBlank
        String skuName,

        @NotNull
        @Positive BigDecimal unitPrice,

        @NotNull
        @Positive Integer quantity

) {
}

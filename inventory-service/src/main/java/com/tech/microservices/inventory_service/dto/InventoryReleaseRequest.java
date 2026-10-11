package com.tech.microservices.inventory_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record InventoryReleaseRequest(
        @NotBlank
        @Size(max = 64)
        String orderNo,

        @NotBlank
        @Size(max = 64)
        String skuId,

        @NotNull
        @Positive
        Long releaseQuantity) {
}

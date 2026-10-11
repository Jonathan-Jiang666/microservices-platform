package com.tech.microservices.inventory_service.dto;

import jakarta.validation.constraints.*;

public record InventoryReservationRequest(

        @NotBlank
        @Size(max = 64)
        String orderNo,

        @NotBlank
        @Size(max = 64)
        String skuId,

        @NotNull
        @Positive
        Long reservedQuantity
) {
}

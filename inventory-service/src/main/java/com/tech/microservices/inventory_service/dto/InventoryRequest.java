package com.tech.microservices.inventory_service.dto;

import jakarta.persistence.Column;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDateTime;

public record InventoryRequest(
        @NotBlank
        @Size(max = 64)
        String skuId,

        @NotNull
        @PositiveOrZero
        Long onHandQuantity

) {
}



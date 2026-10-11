package com.tech.microservices.product.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProductRequest(
    @NotBlank(message = "productId cannot be blank")
    String productId,
    @NotBlank(message = "brand cannot be blank")
    String brand,
    @NotBlank(message = "name cannot be blank")
    String name, 
    String description, 
    @NotNull(message = "price cannot be null")
    BigDecimal price, 
    @NotBlank(message = "category cannot be blank")
    String category,
    @NotBlank(message = "status cannot be blank")
    String status
) {
}

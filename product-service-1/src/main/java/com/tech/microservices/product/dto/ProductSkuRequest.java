package com.tech.microservices.product.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProductSkuRequest(
    @NotBlank(message = "skuId cannot be blank")
    String skuId,
    @NotBlank(message = "productId cannot be blank")
    String productId,
    @NotBlank(message = "skuName cannot be blank")
    String skuName,
    String description,
    @NotNull(message = "price cannot be null")
    BigDecimal price,
    @NotBlank(message = "category cannot be blank")
    String category,
    @NotBlank(message = "status cannot be blank")
    String status
){}

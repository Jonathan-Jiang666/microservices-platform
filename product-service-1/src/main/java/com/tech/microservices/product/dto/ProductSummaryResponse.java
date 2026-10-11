package com.tech.microservices.product.dto;

import java.math.BigDecimal;
public record ProductSummaryResponse(
     String productId,
     String brand,
     String name,
     String description,
     BigDecimal price,
     String category,
     String status
) {
}
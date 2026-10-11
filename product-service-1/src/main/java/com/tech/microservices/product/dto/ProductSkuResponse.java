package com.tech.microservices.product.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductSkuResponse(
    String skuId,
    String productId,
    String skuName,
    String description,
    BigDecimal price,
    String category,
    String status,
    LocalDateTime createAt,
    LocalDateTime updateAt
) {
}
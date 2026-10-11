package com.tech.microservices.product.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProductDetailResponse(
    String productId,
    String brand,
    String name,
    String description, 
    BigDecimal price, 
    String category,
    String status
   // List<ProductSkuResponse> skus
) {
}

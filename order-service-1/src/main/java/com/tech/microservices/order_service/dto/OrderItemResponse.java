package com.tech.microservices.order_service.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
        String skuId,
        String skuName,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal subtotal
) {

}

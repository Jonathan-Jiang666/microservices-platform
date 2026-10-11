package com.tech.microservices.product.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(value = "product_sku")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class ProductSKU {
    @Id
    private String skuId;
    private String productId;
    private String skuName;
    private String description;
    private BigDecimal price;
    private String category;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updateAt;

}

package com.tech.microservices.product.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.tech.microservices.product.dto.ProductSkuRequest;
import com.tech.microservices.product.dto.ProductSkuResponse;
import com.tech.microservices.product.model.Product;
import com.tech.microservices.product.model.ProductSKU;
import com.tech.microservices.product.repository.ProductRepository;
import com.tech.microservices.product.repository.ProductSkuRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductSkuService {

    private final ProductSkuRepository productSkuRepository;
    private final ProductRepository productRepository;

    public ProductSkuResponse createProductSku(ProductSkuRequest productSkuRequest) {
        Product product  = this.productRepository.findByProductId(productSkuRequest.productId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + productSkuRequest.productId()));

        if (productSkuRepository.existsBySkuId(productSkuRequest.skuId())) {
            throw new IllegalArgumentException("Product SKU already exists with skuId: " + productSkuRequest.skuId());
        }

        ProductSKU productSku = ProductSKU.builder()
                .skuId(productSkuRequest.skuId())
                .productId(product.getProductId())
                .skuName(productSkuRequest.skuName())
                .description(productSkuRequest.description())
                .price(productSkuRequest.price())
                .category(productSkuRequest.category())
                .status(productSkuRequest.status())
                .createdAt(LocalDateTime.now())
                .updateAt(LocalDateTime.now())
                .build();
        ProductSKU savedProductSku = productSkuRepository.save(productSku);
        return toProductSkuResponse(savedProductSku);
    }



    public Page<ProductSkuResponse> getProductSkuByProductId(String productId, Pageable pageable)
    {
         int pageSize = Math.min(pageable.getPageSize(), 20);

        // Create a safe Pageable object that handles invalid page numbers and sorts
        Pageable safePageable = PageRequest.of(
               pageable.getPageNumber(),
               pageSize, pageable.getSort()
        );
        Page<ProductSKU> productSkuPage = this.productSkuRepository.findByProductId(productId, safePageable);
        return productSkuPage.map(this::toProductSkuResponse);
    }
    

    public ProductSkuResponse getProductSkuBySkuId(String skuId) {
        ProductSKU productSku = this.productSkuRepository.findBySkuId(skuId)
                .orElseThrow(() -> new IllegalArgumentException("Product SKU not found with id: " + skuId));
        return toProductSkuResponse(productSku);
    }




    private ProductSkuResponse toProductSkuResponse(ProductSKU productSku) {
        return new ProductSkuResponse(
                productSku.getSkuId(),
                productSku.getProductId(),
                productSku.getSkuName(),
                productSku.getDescription(),
                productSku.getPrice(),
                productSku.getCategory(),
                productSku.getStatus(),
                productSku.getCreatedAt(),
                productSku.getUpdateAt()
        );
    }
}

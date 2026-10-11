package com.tech.microservices.product.controller;

import com.tech.microservices.product.dto.ProductSkuRequest;
import com.tech.microservices.product.dto.ProductSkuResponse;
import com.tech.microservices.product.service.ProductSkuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProductSkuController {

    private final ProductSkuService productSkuService;

    @PostMapping("/product-skus")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductSkuResponse createProductSku(@Valid @RequestBody ProductSkuRequest productSkuRequest) {
        return productSkuService.createProductSku(productSkuRequest);
    }

    @GetMapping("/products/{productId}/skus")
    @ResponseStatus(HttpStatus.OK)
    public Page<ProductSkuResponse> getProductSkusByProductId(
            @PathVariable String productId,
            @PageableDefault(size = 20, sort = "price") Pageable pageable
    ) {
        return productSkuService.getProductSkuByProductId(productId, pageable);
    }

    @GetMapping("/skus/{skuId}")
    @ResponseStatus(HttpStatus.OK)
    public ProductSkuResponse getProductSkuBySkuId(@PathVariable String skuId) {
        return productSkuService.getProductSkuBySkuId(skuId);
    }
}

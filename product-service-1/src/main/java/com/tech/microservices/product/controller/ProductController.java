package com.tech.microservices.product.controller;

import com.tech.microservices.product.dto.ProductDetailResponse;
import com.tech.microservices.product.dto.ProductRequest;
import com.tech.microservices.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    //Create new product
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDetailResponse createProduct(@Valid @RequestBody ProductRequest productRequest) {
      return productService.createProduct(productRequest);
    }

    //Get all products by page size and sort, default page size is 20
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Page<ProductDetailResponse> getAllProducts(@PageableDefault(size = 20, sort = "price") Pageable pageable) {
        return productService.getAllProducts(pageable);
    }

}

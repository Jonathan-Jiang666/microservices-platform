package com.tech.microservices.product.service;

import com.tech.microservices.product.dto.ProductDetailResponse;
import com.tech.microservices.product.dto.ProductRequest;
import com.tech.microservices.product.model.Product;
import com.tech.microservices.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;

    public ProductDetailResponse createProduct(ProductRequest productRequest) {

        if (productRepository.existsByProductId(productRequest.productId())) {
            throw new IllegalArgumentException("Product already exists with productId: " + productRequest.productId());
        }

        Product product = Product.builder()
                .productId(productRequest.productId())
                .brand(productRequest.brand())
                .name(productRequest.name())
                .description(productRequest.description())
                .price(productRequest.price())
                .category(productRequest.category())
                .status(productRequest.status())
                .build();
        productRepository.save(product);
        log.info("Product {} created successfully", product.getId());
        return new ProductDetailResponse(
                product.getProductId(),
                product.getBrand(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getCategory(),
                product.getStatus()
        );
    }


    //Get all products by page size and sort, default page size is under 20
    public Page<ProductDetailResponse> getAllProducts(Pageable pageable) {
//        if(pageable.getPageSize() > 20) {
//            throw new IllegalArgumentException("Page size should be less than or equal to 20");
//        }  // Because the default page size is 20, and it is already setting in application.properties, so this check is not needed

        int pageSize = Math.min(pageable.getPageSize(), 20);

        // Create a safe Pageable object that handles invalid page numbers and sorts
        Pageable safePageable = PageRequest.of(
               pageable.getPageNumber(),
               pageSize, pageable.getSort()
        );

        Page<Product> products = this.productRepository.findAll(safePageable);
        log.info("Products fetched successfully, and this time fetched amount is {} ", products.getSize());
        return products.map(product -> new ProductDetailResponse(
                product.getProductId(),
                product.getBrand(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getCategory(),
                product.getStatus()

        ));
    }
}

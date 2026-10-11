package com.tech.microservices.product.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import com.tech.microservices.product.model.ProductSKU;

import java.util.Optional;

public interface ProductSkuRepository extends MongoRepository<ProductSKU, String>, PagingAndSortingRepository<ProductSKU, String> {

     Page<ProductSKU> findByProductId(String productId, Pageable pageable);

     Optional<ProductSKU> findBySkuId(String skuId);

     boolean existsBySkuId(String skuId);
    
}

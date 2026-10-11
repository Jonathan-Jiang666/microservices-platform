package com.tech.microservices.product.repository;

import com.tech.microservices.product.model.Product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;

public interface ProductRepository extends MongoRepository<Product, String>, PagingAndSortingRepository<Product, String>
{
     Page<Product> findAll(Pageable pageable);

     Optional<Product> findByProductId(String productId);

     boolean existsByProductId(String productId);

}

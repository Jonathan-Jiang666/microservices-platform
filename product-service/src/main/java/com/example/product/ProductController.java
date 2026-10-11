package com.example.product;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @GetMapping
    public List<Product> listProducts() {
        return List.of(
                new Product("SKU-100", "Keyboard", new BigDecimal("49.99")),
                new Product("SKU-200", "Mouse", new BigDecimal("29.99"))
        );
    }

    public record Product(String sku, String name, BigDecimal price) {}
}

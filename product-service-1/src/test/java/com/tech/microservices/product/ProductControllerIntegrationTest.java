package com.tech.microservices.product;

import com.tech.microservices.product.dto.ProductDetailResponse;
import com.tech.microservices.product.dto.ProductRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;

/*
 * Integration test using TestRestTemplate against the random web server port.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Import(TestcontainersConfiguration.class)
class ProductControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldCreateProduct() {
        String productId = "prod-test-" + UUID.randomUUID();
        ProductRequest productRequest = new ProductRequest(
                productId,
                "test-brand",
                "Jonathan",
                "test for my description",
                new BigDecimal("100.01"),
                "person",
                "ACTIVE");
        ResponseEntity<ProductDetailResponse> response = restTemplate
                .postForEntity("/api/product", productRequest, ProductDetailResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ProductDetailResponse productResponse = response.getBody();
        assertThat(productResponse).isNotNull();
        assertThat(productResponse.productId()).isEqualTo(productId);
        assertThat(productResponse.brand()).isEqualTo("test-brand");
        assertThat(productResponse.name()).isEqualTo("Jonathan");
        assertThat(productResponse.description()).isEqualTo("test for my description");
        assertThat(productResponse.price()).isEqualByComparingTo("100.01");
        assertThat(productResponse.category()).isEqualTo("person");
        assertThat(productResponse.status()).isEqualTo("ACTIVE");

    }
}

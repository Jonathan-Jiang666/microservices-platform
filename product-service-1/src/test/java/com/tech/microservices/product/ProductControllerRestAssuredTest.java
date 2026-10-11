package com.tech.microservices.product;

import io.restassured.RestAssured;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.mongodb.MongoDBContainer;

import java.util.UUID;

/*
 * this is integration test and used RestAssured test framework
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
public class ProductControllerRestAssuredTest {
//    @ServiceConnection   -- it's conflict with @Import(TestcontainersConfiguration.class)
//    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0.5");

    @LocalServerPort
    private Integer port;

    @BeforeEach
    void setup()
    {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }


    @Test
    void shouldCreateProduct() {
        String productId = "prod-test-" + UUID.randomUUID();
        String requestBody = """
				  {
				      "productId": "%s",
				      "brand": "test-brand",
				      "name": "Jonathan" ,
				      "description":  "test for my description" ,
				      "price" : 100.01 ,
				      "category": "person",
				      "status": "ACTIVE"
				  }
				  """.formatted(productId);
        RestAssured.given()
                .contentType("application/json")
                .body(requestBody)
                .when()
                .post("/api/product")
                .then()
                .statusCode(201)
                .body("productId", Matchers.is(productId))
                .body("brand", Matchers.is("test-brand"))
                .body("name", Matchers.is("Jonathan"))
                .body("description", Matchers.is("test for my description"))
                .body("price", Matchers.is(100.01f))
                .body("category", Matchers.is("person"))
                .body("status", Matchers.is("ACTIVE"));
    }
}

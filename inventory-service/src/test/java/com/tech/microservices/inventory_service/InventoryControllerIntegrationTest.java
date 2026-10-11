package com.tech.microservices.inventory_service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.tech.microservices.inventory_service.config.SecurityConfig;
import com.tech.microservices.inventory_service.dto.InventoryRequest;
import com.tech.microservices.inventory_service.dto.InventoryReservationRequest;
import com.tech.microservices.inventory_service.dto.InventoryResponse;
import com.tech.microservices.inventory_service.dto.StockInRequest;
import com.tech.microservices.inventory_service.repository.IdempotencyKeyRepository;
import com.tech.microservices.inventory_service.repository.InventoryRepository;
import com.tech.microservices.inventory_service.repository.InventoryReservationRepository;
import com.tech.microservices.inventory_service.repository.InventoryTransactionRepository;
import com.tech.microservices.inventory_service.repository.OutboxEventRepository;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;


import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({SecurityConfig.class, TestcontainersConfiguration.class})
@AutoConfigureTestRestTemplate
@Slf4j
public class InventoryControllerIntegrationTest {


    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private InventoryTransactionRepository inventoryTransactionRepository;

    @Autowired
    private InventoryReservationRepository inventoryReservationRepository;

    @Autowired
    private IdempotencyKeyRepository idempotencyKeysRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;
   
    @Autowired
    private InventoryRepository inventoryRepository;
    

    @BeforeEach
    void setUp() throws IOException {

    inventoryTransactionRepository.deleteAll();
    inventoryReservationRepository.deleteAll();
    idempotencyKeysRepository.deleteAll();
    outboxEventRepository.deleteAll();
    inventoryRepository.deleteAll();

    }

    @Test
    void shouldCreateNewInventory() throws IOException
    {
        String skuId = newSkuId();
        //String orderNo = newOrderNo();
        String newKey = newIdempotencyKey();
        String json = readJsonFile("create-inventory-request.json");
        json = json.replace("${skuId}", skuId);
        
        HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("idempotencyKey", newKey);
		HttpEntity<String> request = new HttpEntity<>(json, headers);

		ResponseEntity<InventoryResponse> response = this.restTemplate
				.withBasicAuth("admin","admin")  // admin is the role of the ADMIN user
				.postForEntity("/api/inventory", request, InventoryResponse.class); 
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().skuId()).isEqualTo(skuId);
		
    }



    @Test
    void shouldStockInInventory() throws IOException
    {

        String skuId = newSkuId();
        String newKey = newIdempotencyKey();
        String json = readJsonFile("create-inventory-request.json");
        json = json.replace("${skuId}", skuId);
        
        HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("idempotencyKey", newKey);
		HttpEntity<String> request = new HttpEntity<>(json, headers);

		ResponseEntity<InventoryResponse> response = this.restTemplate
				.withBasicAuth("admin","admin")  // admin is the role of the ADMIN user
				.postForEntity("/api/inventory", request, InventoryResponse.class); 
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().skuId()).isEqualTo(skuId);
        assertThat(response.getBody().onHandQuantity()).isEqualTo(100L);
        assertThat(response.getBody().reservedQuantity()).isEqualTo(0L);
        assertThat(response.getBody().availableQuantity()).isEqualTo(100L);
        assertThat(response.getHeaders().getLocation()).isNotNull();
        

        String newStockInKey = newIdempotencyKey();
        String stockInJson = readJsonFile("stock-in-request.json");
        stockInJson = stockInJson.replace("${skuId}", skuId);
        
        HttpHeaders headerStockHeaders = new HttpHeaders();
        headerStockHeaders.setContentType(MediaType.APPLICATION_JSON);
        headerStockHeaders.set("idempotencyKey", newStockInKey);
        HttpEntity<String> requestStockIn = new HttpEntity<>(stockInJson, headerStockHeaders);

        ResponseEntity<InventoryResponse> stockedResponse = this.restTemplate
                .withBasicAuth("admin","admin")  // admin is the role of the ADMIN user
                .postForEntity("/api/inventory/stock-in", requestStockIn, InventoryResponse.class);
        assertThat(stockedResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(stockedResponse.getBody()).isNotNull();
        assertThat(stockedResponse.getBody().skuId()).isEqualTo(skuId);
        assertThat(stockedResponse.getBody().onHandQuantity()).isEqualTo(1100L);
        assertThat(stockedResponse.getBody().availableQuantity()).isEqualTo(1100L);
        assertThat(stockedResponse.getBody().reservedQuantity()).isEqualTo(0L);
        assertThat(stockedResponse.getHeaders().getLocation()).isNotNull();
    }


    @Test
    void shouldReserveInventory() throws IOException
    {
        // Create inventory first
        String skuId = newSkuId();
        String newKey = newIdempotencyKey();
        String json = readJsonFile("create-inventory-request.json");
        json = json.replace("${skuId}", skuId);
        
        HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("idempotencyKey", newKey);
		HttpEntity<String> request = new HttpEntity<>(json, headers);

		ResponseEntity<InventoryResponse> response = this.restTemplate
				.withBasicAuth("admin","admin") 
				.postForEntity("/api/inventory", request, InventoryResponse.class); 
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().skuId()).isEqualTo(skuId);
        assertThat(response.getBody().onHandQuantity()).isEqualTo(100L);
        assertThat(response.getBody().reservedQuantity()).isEqualTo(0L);
        assertThat(response.getBody().availableQuantity()).isEqualTo(100L);
        assertThat(response.getHeaders().getLocation()).isNotNull();

        // Reserve inventory
        String newReserveKey = newIdempotencyKey();
        String orderNo = newOrderNo();


        String reserveJson = readJsonFile("reserve-inventory-request.json");
        reserveJson = reserveJson.replace("${skuId}", skuId);
        reserveJson = reserveJson.replace("${orderNo}", orderNo);

        HttpHeaders reserveHeaders = new HttpHeaders();
        reserveHeaders.setContentType(MediaType.APPLICATION_JSON);
        reserveHeaders.set("idempotencyKey", newReserveKey);
        HttpEntity<String> reserveRequest = new HttpEntity<>(reserveJson, reserveHeaders);

        ResponseEntity<InventoryResponse> reserveResponse = this.restTemplate
                .withBasicAuth("admin","admin") 
                .postForEntity("/api/inventory/reserve", reserveRequest, InventoryResponse.class);
        assertThat(reserveResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(reserveResponse.getBody()).isNotNull();
        assertThat(reserveResponse.getBody().skuId()).isEqualTo(skuId);
        assertThat(reserveResponse.getBody().onHandQuantity()).isEqualTo(100L);
        assertThat(reserveResponse.getBody().reservedQuantity()).isEqualTo(10L);
        assertThat(reserveResponse.getBody().availableQuantity()).isEqualTo(90L);
        assertThat(reserveResponse.getHeaders().getLocation()).isNotNull();

    }

    @Test    
    void shouldConfirmReservation() throws IOException
    {
        // Create inventory first
        String skuId = newSkuId();
        String newKey = newIdempotencyKey();
        String json = readJsonFile("create-inventory-request.json");
        json = json.replace("${skuId}", skuId);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("idempotencyKey", newKey);
        HttpEntity<String> request = new HttpEntity<>(json, headers);

        ResponseEntity<InventoryResponse> response = this.restTemplate
				.withBasicAuth("admin","admin") 
				.postForEntity("/api/inventory", request, InventoryResponse.class); 
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().skuId()).isEqualTo(skuId);
        assertThat(response.getBody().onHandQuantity()).isEqualTo(100L);
        assertThat(response.getBody().reservedQuantity()).isEqualTo(0L);
        assertThat(response.getBody().availableQuantity()).isEqualTo(100L);
        assertThat(response.getHeaders().getLocation()).isNotNull();


        // Reserve inventory
        String newReserveKey = newIdempotencyKey();
        String orderNo = newOrderNo();


        String reserveJson = readJsonFile("reserve-inventory-request.json");
        reserveJson = reserveJson.replace("${skuId}", skuId);
        reserveJson = reserveJson.replace("${orderNo}", orderNo);

        HttpHeaders reserveHeaders = new HttpHeaders();
        reserveHeaders.setContentType(MediaType.APPLICATION_JSON);
        reserveHeaders.set("idempotencyKey", newReserveKey);
        HttpEntity<String> reserveRequest = new HttpEntity<>(reserveJson, reserveHeaders);

        ResponseEntity<InventoryResponse> reserveResponse = this.restTemplate
                .withBasicAuth("admin","admin") 
                .postForEntity("/api/inventory/reserve", reserveRequest, InventoryResponse.class);
        assertThat(reserveResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(reserveResponse.getBody()).isNotNull();
        assertThat(reserveResponse.getBody().skuId()).isEqualTo(skuId);
        assertThat(reserveResponse.getBody().onHandQuantity()).isEqualTo(100L);
        assertThat(reserveResponse.getBody().reservedQuantity()).isEqualTo(10L);
        assertThat(reserveResponse.getBody().availableQuantity()).isEqualTo(90L);
        assertThat(reserveResponse.getHeaders().getLocation()).isNotNull();



        // Confirm reservation
        String confirmJson = readJsonFile("confirm-inventory-request.json");
        confirmJson = confirmJson.replace("${skuId}", skuId);
        confirmJson = confirmJson.replace("${orderNo}", orderNo);

        String newConfirmKey = newIdempotencyKey();
        HttpHeaders confirmHeaders = new HttpHeaders();
        confirmHeaders.setContentType(MediaType.APPLICATION_JSON);
        confirmHeaders.set("idempotencyKey", newConfirmKey);
        HttpEntity<String> confirmRequest = new HttpEntity<>(confirmJson, confirmHeaders);

        ResponseEntity<InventoryResponse> confirmResponse = this.restTemplate
                .withBasicAuth("admin","admin") 
                .postForEntity("/api/inventory/confirm", confirmRequest, InventoryResponse.class);
        assertThat(confirmResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(confirmResponse.getBody()).isNotNull();
        assertThat(confirmResponse.getBody().skuId()).isEqualTo(skuId);
        assertThat(confirmResponse.getBody().onHandQuantity()).isEqualTo(90L);
        assertThat(confirmResponse.getBody().reservedQuantity()).isEqualTo(0L);
        assertThat(confirmResponse.getBody().availableQuantity()).isEqualTo(90L);
        assertThat(confirmResponse.getHeaders().getLocation()).isNotNull();

    }


    @Test
    void shouldReleaseInventory() throws IOException
    {
        // Create inventory first
        String skuId = newSkuId();
        String newKey = newIdempotencyKey();
        String json = readJsonFile("create-inventory-request.json");
        json = json.replace("${skuId}", skuId);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("idempotencyKey", newKey);
        HttpEntity<String> request = new HttpEntity<>(json, headers);

        ResponseEntity<InventoryResponse> response = this.restTemplate
				.withBasicAuth("admin","admin") 
				.postForEntity("/api/inventory", request, InventoryResponse.class); 
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().skuId()).isEqualTo(skuId);
        assertThat(response.getBody().onHandQuantity()).isEqualTo(100L);
        assertThat(response.getBody().reservedQuantity()).isEqualTo(0L);
        assertThat(response.getBody().availableQuantity()).isEqualTo(100L);
        assertThat(response.getHeaders().getLocation()).isNotNull();


        // Reserve inventory
       
        String orderNo = newOrderNo();

        String newReserveKey = newIdempotencyKey();
        String reserveJson = readJsonFile("reserve-inventory-request.json");
        reserveJson = reserveJson.replace("${skuId}", skuId);
        reserveJson = reserveJson.replace("${orderNo}", orderNo);

        HttpHeaders reserveHeaders = new HttpHeaders();
        reserveHeaders.setContentType(MediaType.APPLICATION_JSON);
        reserveHeaders.set("idempotencyKey", newReserveKey);
        HttpEntity<String> reserveRequest = new HttpEntity<>(reserveJson, reserveHeaders);

        ResponseEntity<InventoryResponse> reserveResponse = this.restTemplate
                .withBasicAuth("admin","admin") 
                .postForEntity("/api/inventory/reserve", reserveRequest, InventoryResponse.class);
        assertThat(reserveResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(reserveResponse.getBody()).isNotNull();
        assertThat(reserveResponse.getBody().skuId()).isEqualTo(skuId);
        assertThat(reserveResponse.getBody().onHandQuantity()).isEqualTo(100L);
        assertThat(reserveResponse.getBody().reservedQuantity()).isEqualTo(10L);
        assertThat(reserveResponse.getBody().availableQuantity()).isEqualTo(90L);
        assertThat(reserveResponse.getHeaders().getLocation()).isNotNull();


        // Release reservation
        String releaseJson = readJsonFile("release-inventory-request.json");
        releaseJson = releaseJson.replace("${skuId}", skuId);
        releaseJson = releaseJson.replace("${orderNo}", orderNo);

        String newReleaseKey = newIdempotencyKey();
        HttpHeaders releaseHeaders = new HttpHeaders();
        releaseHeaders.setContentType(MediaType.APPLICATION_JSON);
        releaseHeaders.set("idempotencyKey", newReleaseKey);
        HttpEntity<String> releaseRequest = new HttpEntity<>(releaseJson, releaseHeaders);

        ResponseEntity<InventoryResponse> releaseResponse = this.restTemplate
                .withBasicAuth("admin","admin")
                .postForEntity("/api/inventory/release", releaseRequest, InventoryResponse.class);
        assertThat(releaseResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(releaseResponse.getBody()).isNotNull();
        assertThat(releaseResponse.getBody().skuId()).isEqualTo(skuId);
        assertThat(releaseResponse.getBody().onHandQuantity()).isEqualTo(100L);
        assertThat(releaseResponse.getBody().reservedQuantity()).isEqualTo(0L);
        assertThat(releaseResponse.getBody().availableQuantity()).isEqualTo(100L);
        assertThat(releaseResponse.getHeaders().getLocation()).isNotNull();
    }


    @Test
    void shouldGetInventoryBySkuId() throws IOException {
        // Create inventory first
        String skuId = newSkuId();
        String newKey = newIdempotencyKey();
        String json = readJsonFile("create-inventory-request.json");
        json = json.replace("${skuId}", skuId);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("idempotencyKey", newKey);
        HttpEntity<String> request = new HttpEntity<>(json, headers);

        ResponseEntity<InventoryResponse> response = this.restTemplate
				.withBasicAuth("admin","admin") 
				.postForEntity("/api/inventory", request, InventoryResponse.class); 
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().skuId()).isEqualTo(skuId);
        assertThat(response.getBody().onHandQuantity()).isEqualTo(100L);
        assertThat(response.getBody().reservedQuantity()).isEqualTo(0L);
        assertThat(response.getBody().availableQuantity()).isEqualTo(100L);
        assertThat(response.getHeaders().getLocation()).isNotNull();

        // Then call GET /api/inventory/{skuId} and assert the response
        ResponseEntity<InventoryResponse> getResponse = this.restTemplate
                .withBasicAuth("admin","admin") 
                .getForEntity("/api/inventory/{skuId}", InventoryResponse.class, skuId);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();
        assertThat(getResponse.getBody().skuId()).isEqualTo(skuId);
        assertThat(getResponse.getBody().onHandQuantity()).isEqualTo(100L);
        assertThat(getResponse.getBody().reservedQuantity()).isEqualTo(0L);
        assertThat(getResponse.getBody().availableQuantity()).isEqualTo(100L);
    }



    @Test
    void shouldGetAllInventories() throws IOException {
        // Create multiple inventories
        String skuId = newSkuId();
        String newKey = newIdempotencyKey();
        String json = readJsonFile("create-inventory-request.json");
        json = json.replace("${skuId}", skuId);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("idempotencyKey", newKey);
        HttpEntity<String> request = new HttpEntity<>(json, headers);

        ResponseEntity<InventoryResponse> response = this.restTemplate
				.withBasicAuth("admin","admin") 
				.postForEntity("/api/inventory", request, InventoryResponse.class); 
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().skuId()).isEqualTo(skuId);
        assertThat(response.getBody().onHandQuantity()).isEqualTo(100L);
        assertThat(response.getBody().reservedQuantity()).isEqualTo(0L);
        assertThat(response.getBody().availableQuantity()).isEqualTo(100L);
        assertThat(response.getHeaders().getLocation()).isNotNull();

        String skuId2 = newSkuId();
        String newKey2 = newIdempotencyKey();
        String json2 = readJsonFile("create-inventory-request.json");
        json2 = json2.replace("${skuId}", skuId2);
        
        HttpHeaders headers2 = new HttpHeaders();
        headers2.setContentType(MediaType.APPLICATION_JSON);
        headers2.set("idempotencyKey", newKey2);
        HttpEntity<String> request2 = new HttpEntity<>(json2, headers2);

        ResponseEntity<InventoryResponse> response2 = this.restTemplate
				.withBasicAuth("admin","admin") 
				.postForEntity("/api/inventory", request2, InventoryResponse.class); 
		assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response2.getBody()).isNotNull();
        assertThat(response2.getBody().skuId()).isEqualTo(skuId2);
        assertThat(response2.getBody().onHandQuantity()).isEqualTo(100L);
        assertThat(response2.getBody().reservedQuantity()).isEqualTo(0L);
        assertThat(response2.getBody().availableQuantity()).isEqualTo(100L);
        assertThat(response2.getHeaders().getLocation()).isNotNull();

        // Now get all inventories
        // Then call GET /api/inventory and assert the response

        ResponseEntity<String> getAllResponse = this.restTemplate
                .withBasicAuth("admin","admin") 
                .getForEntity("/api/inventory", String.class);
        assertThat(getAllResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getAllResponse.getBody()).isNotNull();
        assertThat(getAllResponse.getBody()).contains(skuId);
        assertThat(getAllResponse.getBody()).contains(skuId2);
    }

    @Test
    void shouldGetAllReservationsBySkuId() throws IOException {
        // Create inventory first
        String skuId = newSkuId();
        String newKey = newIdempotencyKey();
        String json = readJsonFile("create-inventory-request.json");
        json = json.replace("${skuId}", skuId);
        
        HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("idempotencyKey", newKey);
		HttpEntity<String> request = new HttpEntity<>(json, headers);

		ResponseEntity<InventoryResponse> response = this.restTemplate
				.withBasicAuth("admin","admin") 
				.postForEntity("/api/inventory", request, InventoryResponse.class); 
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().skuId()).isEqualTo(skuId);
        assertThat(response.getBody().onHandQuantity()).isEqualTo(100L);
        assertThat(response.getBody().reservedQuantity()).isEqualTo(0L);
        assertThat(response.getBody().availableQuantity()).isEqualTo(100L);
        assertThat(response.getHeaders().getLocation()).isNotNull();

        // Reserve inventory frist time
        String newReserveKey = newIdempotencyKey();
        String orderNo = newOrderNo();


        String reserveJson = readJsonFile("reserve-inventory-request.json");
        reserveJson = reserveJson.replace("${skuId}", skuId);
        reserveJson = reserveJson.replace("${orderNo}", orderNo);

        HttpHeaders reserveHeaders = new HttpHeaders();
        reserveHeaders.setContentType(MediaType.APPLICATION_JSON);
        reserveHeaders.set("idempotencyKey", newReserveKey);
        HttpEntity<String> reserveRequest = new HttpEntity<>(reserveJson, reserveHeaders);

        ResponseEntity<InventoryResponse> reserveResponse = this.restTemplate
                .withBasicAuth("admin","admin") 
                .postForEntity("/api/inventory/reserve", reserveRequest, InventoryResponse.class);
        assertThat(reserveResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(reserveResponse.getBody()).isNotNull();
        assertThat(reserveResponse.getBody().skuId()).isEqualTo(skuId);
        assertThat(reserveResponse.getBody().onHandQuantity()).isEqualTo(100L);
        assertThat(reserveResponse.getBody().reservedQuantity()).isEqualTo(10L);
        assertThat(reserveResponse.getBody().availableQuantity()).isEqualTo(90L);
        assertThat(reserveResponse.getHeaders().getLocation()).isNotNull();
        
        // Reserve inventory second time
        String newReserveKey2 = newIdempotencyKey();
        String orderNo2 = newOrderNo();


        String reserveJson2 = readJsonFile("reserve-inventory-request.json");
        reserveJson2 = reserveJson2.replace("${skuId}", skuId);
        reserveJson2 = reserveJson2.replace("${orderNo}", orderNo2);

        HttpHeaders reserveHeaders2 = new HttpHeaders();
        reserveHeaders2.setContentType(MediaType.APPLICATION_JSON);
        reserveHeaders2.set("idempotencyKey", newReserveKey2);
        HttpEntity<String> reserveRequest2 = new HttpEntity<>(reserveJson2, reserveHeaders2);

        ResponseEntity<InventoryResponse> reserveResponse2 = this.restTemplate
                .withBasicAuth("admin","admin") 
                .postForEntity("/api/inventory/reserve", reserveRequest2, InventoryResponse.class);
        assertThat(reserveResponse2.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(reserveResponse2.getBody()).isNotNull();
        assertThat(reserveResponse2.getBody().skuId()).isEqualTo(skuId);
        assertThat(reserveResponse2.getBody().onHandQuantity()).isEqualTo(100L);
        assertThat(reserveResponse2.getBody().reservedQuantity()).isEqualTo(20L);
        assertThat(reserveResponse2.getBody().availableQuantity()).isEqualTo(80L);
        assertThat(reserveResponse2.getHeaders().getLocation()).isNotNull(); 

        // Then call GET /api/inventory/{skuId}/reservations and assert the
        ResponseEntity<String> getReservationsResponse = this.restTemplate
                .withBasicAuth("admin","admin") 
                .getForEntity("/api/inventory/{skuId}/reservations", String.class, skuId);
        assertThat(getReservationsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getReservationsResponse.getBody()).isNotNull();
        assertThat(getReservationsResponse.getBody()).contains(orderNo);
        assertThat(getReservationsResponse.getBody()).contains(orderNo2);
        assertThat(getReservationsResponse.getBody()).contains(skuId);
    }


    @Test
    void shouldGetAllTransactionsBySkuId() throws IOException {
       
    // Similar to the previous test, create inventory and perform stock-in, reserve, confirm, and release operations
    // Create inventory first
        String skuId = newSkuId();
        String newKey = newIdempotencyKey();
        String json = readJsonFile("create-inventory-request.json");
        json = json.replace("${skuId}", skuId);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("idempotencyKey", newKey);
        HttpEntity<String> request = new HttpEntity<>(json, headers);

        ResponseEntity<InventoryResponse> response = this.restTemplate
				.withBasicAuth("admin","admin") 
				.postForEntity("/api/inventory", request, InventoryResponse.class); 
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().skuId()).isEqualTo(skuId);
        assertThat(response.getBody().onHandQuantity()).isEqualTo(100L);
        assertThat(response.getBody().reservedQuantity()).isEqualTo(0L);
        assertThat(response.getBody().availableQuantity()).isEqualTo(100L);
        assertThat(response.getHeaders().getLocation()).isNotNull();


        // Reserve inventory
        String newReserveKey = newIdempotencyKey();
        String orderNo = newOrderNo();


        String reserveJson = readJsonFile("reserve-inventory-request.json");
        reserveJson = reserveJson.replace("${skuId}", skuId);
        reserveJson = reserveJson.replace("${orderNo}", orderNo);

        HttpHeaders reserveHeaders = new HttpHeaders();
        reserveHeaders.setContentType(MediaType.APPLICATION_JSON);
        reserveHeaders.set("idempotencyKey", newReserveKey);
        HttpEntity<String> reserveRequest = new HttpEntity<>(reserveJson, reserveHeaders);

        ResponseEntity<InventoryResponse> reserveResponse = this.restTemplate
                .withBasicAuth("admin","admin") 
                .postForEntity("/api/inventory/reserve", reserveRequest, InventoryResponse.class);
        assertThat(reserveResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(reserveResponse.getBody()).isNotNull();
        assertThat(reserveResponse.getBody().skuId()).isEqualTo(skuId);
        assertThat(reserveResponse.getBody().onHandQuantity()).isEqualTo(100L);
        assertThat(reserveResponse.getBody().reservedQuantity()).isEqualTo(10L);
        assertThat(reserveResponse.getBody().availableQuantity()).isEqualTo(90L);
        assertThat(reserveResponse.getHeaders().getLocation()).isNotNull();



        // Confirm reservation
        String confirmJson = readJsonFile("confirm-inventory-request.json");
        confirmJson = confirmJson.replace("${skuId}", skuId);
        confirmJson = confirmJson.replace("${orderNo}", orderNo);

        String newConfirmKey = newIdempotencyKey();
        HttpHeaders confirmHeaders = new HttpHeaders();
        confirmHeaders.setContentType(MediaType.APPLICATION_JSON);
        confirmHeaders.set("idempotencyKey", newConfirmKey);
        HttpEntity<String> confirmRequest = new HttpEntity<>(confirmJson, confirmHeaders);

        ResponseEntity<InventoryResponse> confirmResponse = this.restTemplate
                .withBasicAuth("admin","admin") 
                .postForEntity("/api/inventory/confirm", confirmRequest, InventoryResponse.class);
        assertThat(confirmResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(confirmResponse.getBody()).isNotNull();
        assertThat(confirmResponse.getBody().skuId()).isEqualTo(skuId);
        assertThat(confirmResponse.getBody().onHandQuantity()).isEqualTo(90L);
        assertThat(confirmResponse.getBody().reservedQuantity()).isEqualTo(0L);
        assertThat(confirmResponse.getBody().availableQuantity()).isEqualTo(90L);
        assertThat(confirmResponse.getHeaders().getLocation()).isNotNull();


        // Then call GET /api/inventory/{skuId}/transactions and assert the

        ResponseEntity<String> getTransactionsResponse = this.restTemplate
                .withBasicAuth("admin","admin") 
                .getForEntity("/api/inventory/{skuId}/transactions", String.class, skuId);
        assertThat(getTransactionsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getTransactionsResponse.getBody()).isNotNull();
        assertThat(getTransactionsResponse.getBody()).contains(skuId);
        assertThat(getTransactionsResponse.getBody()).contains(orderNo);
        assertThat(getTransactionsResponse.getBody()).contains("RESERVE");
        log.info("getTransactionsResponse.getBody(): {}", getTransactionsResponse.getBody());
        assertThat(getTransactionsResponse.getBody()).contains("SALE");
        assertThat(getTransactionsResponse.getBody()).contains("INITIAL_STOCK");

        // then use orderNo to get transactions by orderNo
        ResponseEntity<String> getTransactionsByOrderNoResponse = this.restTemplate
                .withBasicAuth("admin","admin") 
                .getForEntity("/api/inventory/orderNo/{orderNo}/transactions", String.class, orderNo);
        assertThat(getTransactionsByOrderNoResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getTransactionsByOrderNoResponse.getBody()).isNotNull();
        assertThat(getTransactionsByOrderNoResponse.getBody()).contains(skuId);
        log.info("getTransactionsByOrderNoResponse.getBody(): {}", getTransactionsByOrderNoResponse.getBody());
        assertThat(getTransactionsByOrderNoResponse.getBody()).contains(orderNo);
        assertThat(getTransactionsByOrderNoResponse.getBody()).contains("RESERVE");
        assertThat(getTransactionsByOrderNoResponse.getBody()).contains("SALE");
    }


    private String readJsonFile(String filePath) throws IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filePath);
        if (inputStream == null) {
            throw new IOException("File not found: " + filePath);
        }
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }



   private String newSkuId() {
        return "sku-" + UUID.randomUUID().toString();
    }

    private String newOrderNo() {
        return "order-" + UUID.randomUUID().toString();
    }

    private String newIdempotencyKey() {
        return "idemKey-"+UUID.randomUUID().toString();
    }
    
    
}

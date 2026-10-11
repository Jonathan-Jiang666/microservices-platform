package com.tech.microservices.order_service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.context.annotation.Import;

import com.tech.microservices.order_service.dto.OrderResponse;
import com.tech.microservices.order_service.model.Order;
import com.tech.microservices.order_service.model.OrderItems;
import com.tech.microservices.order_service.model.OrderStatus;
import com.tech.microservices.order_service.repository.IdempotencyKeysRepository;
import com.tech.microservices.order_service.repository.OrderRepository;
import com.tech.microservices.order_service.repository.OutboxEventRepository;



/**
 * Order Consumer Contract Test。
 * Kafka 消费者端的 Contract 测试，验证 Order Service 消费 Inventory Service 事件的行为
 *
 * WireMock / Stub Runner 的作用：
 * 1. 从本地 Maven 仓库加载 Inventory Service 生成的 stubs.jar。
 * 2. Stub Runner 在测试期间启动 WireMock HTTP 服务。
 * 3. Order Service 的 Feign Client 会像真实运行时一样发送 HTTP 请求。
 * 4. WireMock 根据 Contract 生成的 URL、Header 和 Body mapping
 *    匹配请求，并返回约定的状态码和响应体。
 *
 * 优点：
 * - 不需要启动真正的 Inventory Service。
 * - 能测试 Feign 请求路径、请求头、JSON 序列化和异常状态转换。
 * - Stub 来源于 Inventory Provider Contract，降低两个服务接口不一致的风险。
 * - 比完整端到端测试更快、更稳定。
 *
 * 注意：
 * 这不是完整的端到端测试，不会验证 Inventory 数据库、Kafka、
 * InventoryService 真实业务逻辑或真实部署网络。
 */

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureTestRestTemplate
// 加载 Inventory Contract Stub，并由 Stub Runner 启动 WireMock 监听 8082
@AutoConfigureStubRunner(
        ids = "com.tech.microservices:inventory-service:0.0.1-SNAPSHOT:stubs:8082",
        stubsMode = StubRunnerProperties.StubsMode.LOCAL
)
@Import(TestcontainersConfiguration.class)
public class OrderInventoryContractTest {

@Autowired
private TestRestTemplate restTemplate;

@Autowired
private OutboxEventRepository outboxEventRepository;

@Autowired 
private IdempotencyKeysRepository idempotencyKeysRepository;

@Autowired 
private OrderRepository orderRepository;

@BeforeEach 
void cleanDatabase(){
   
        outboxEventRepository.deleteAllInBatch();
        idempotencyKeysRepository.deleteAllInBatch();
        orderRepository.deleteAllInBatch();
}


@Test
void shouldCreateOrderWithInventoryStub() throws Exception {
    String json = readJsonFile("/create-order-request.json");

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("idempotencyKey", "post-order-contract-001");

    HttpEntity<String> request = new HttpEntity<>(json, headers);

    ResponseEntity<OrderResponse> response = restTemplate
            .withBasicAuth("admin", "admin")
            .postForEntity("/api/orders", request, OrderResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().orderNumber()).isEqualTo("order-contract-001");
   }

    private String readJsonFile(String path) throws Exception {
    InputStream inputStream = getClass().getResourceAsStream(path);
    assertThat(inputStream).isNotNull();
    return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
   }


@Test
void shouldInsufficientInventoryStub() throws Exception {
    String json = readJsonFile("/create-order-insufficient-inventory-request.json");

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("idempotencyKey", "post-order-insufficient-001");

    HttpEntity<String> request = new HttpEntity<>(json, headers);

    ResponseEntity<String> response = restTemplate
            .withBasicAuth("admin", "admin")
            .postForEntity("/api/orders", request, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).contains("INSUFFICIENT_INVENTORY");
   }


// Order cancellation and inventory release contract tests
@Test
void shouldReleaseReservationStub() throws Exception {
   
     // First, create an order to reserve inventory
    OrderResponse orderResponse = givenNewInventoryOrder();


    // Now, release the reservation for the order
    HttpHeaders releaseHeaders = new HttpHeaders();
    releaseHeaders.setContentType(MediaType.APPLICATION_JSON);
    releaseHeaders.set("idempotencyKey",
     "cancel-order-contract-001");

    HttpEntity<String> releaseRequest = new HttpEntity<>(null, releaseHeaders);

    ResponseEntity<String> releaseResponse = restTemplate
            .withBasicAuth("admin", "admin")
            .exchange(
                "/api/orders/{orderNumber}/cancel", 
                HttpMethod.PATCH, 
                releaseRequest,
                 String.class,
                orderResponse.orderNumber());

    assertThat(releaseResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(releaseResponse.getBody()).isNull();
   }

   

// Order 
@Test
void shouldReleaseReservationNotFoundStub() throws Exception {
   
    givenOrderWithoutInventoryReservation();
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("idempotencyKey", "release-order-not-found-sku-contract-001");
    HttpEntity<String> request = new HttpEntity<>(null, headers);

    ResponseEntity<String> response = restTemplate
            .withBasicAuth("admin", "admin")
            .exchange(
                "/api/orders/{orderNumber}/cancel", 
                HttpMethod.PATCH, 
                request,
                 String.class,
                "order-not-found-001");
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).contains("RESERVATION_NOT_FOUND");
   }



@Test
void shouldConfirmReservationStub() throws Exception {
   
    //First, create paid order to reserve inventory
    OrderResponse orderResponse = givenPaidOrder();

    // Now, confirm the reservation for the order
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("idempotencyKey", 
    "confirm-order-contract-001-sku-contract-001");

    HttpEntity<String> request = new HttpEntity<>(null, headers);

    ResponseEntity<String> response = restTemplate
            .withBasicAuth("admin", "admin")
            .exchange(
                "/api/orders/{orderNumber}/inventory/confirm", 
                HttpMethod.PATCH, 
                request,
                 String.class,
                orderResponse.orderNumber());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(response.getBody()).isNull();
   }


@Test
void shouldExpireReservationStub() throws Exception {
   
    // First, create an payment pending order to reserve inventory
    OrderResponse orderResponse = givenPaymentPendingOrder();

    // Now, expire the reservation for the order
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("idempotencyKey", 
    "expire-order-contract-001-sku-contract-001");

    HttpEntity<String> request = new HttpEntity<>(null, headers);

    ResponseEntity<String> response = restTemplate
            .withBasicAuth("admin", "admin")
            .exchange(
                "/api/orders/{orderNumber}/inventory/expire", 
                HttpMethod.PATCH, 
                request,
                 String.class,
                orderResponse.orderNumber());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(response.getBody()).isNull();
   }



   // Create new order
   private OrderResponse givenNewInventoryOrder() throws Exception
   {
        String json = readJsonFile("/create-order-request.json");

        HttpHeaders headers = jsonHeaders("post-order-contract-001");

        ResponseEntity<OrderResponse> response = restTemplate
                .withBasicAuth("admin", "admin")
                .postForEntity("/api/orders", new HttpEntity<>(json, headers), 
                OrderResponse.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        return response.getBody();
   }

  // Create new order with payment pending status
   private OrderResponse givenPaymentPendingOrder() throws Exception
   {
        OrderResponse orderResponse = givenNewInventoryOrder();
        updateStatus(orderResponse.orderNumber(), OrderStatus.PAYMENT_PENDING, "update-order-status-contract-001");
        return getOrder(orderResponse.orderNumber());
   }


   // Create new order without inventory reservation
   private Order givenOrderWithoutInventoryReservation() throws Exception
   {
      Order order = Order.builder()
            .orderNumber("order-not-found-001")
            .userId(1001L)
            .orderStatus(OrderStatus.INVENTORY_RESERVED)
            .totalAmount(new BigDecimal("79900.00"))
            .currency("USD")
            .idempotencyKey("seed-order-not-found-001")
            .shippingAddress("Dublin")
            .billingAddress("Dublin")
            .recipientName("Jonathan")
            .recipientEmail("jonathan@test.com")
            .recipientPhone("0871042230")
            .orderItems(new ArrayList<>())
            .build();

    OrderItems item = OrderItems.builder()
            .order(order)
            .skuId("sku-contract-001")
            .skuName("contract sku")
            .unitPrice(new BigDecimal("799.00"))
            .quantity(100)
            .subtotal(new BigDecimal("79900.00"))
            .build();

    order.getOrderItems().add(item);

    return orderRepository.save(order);
   }

   // Create new order with paid status
   private OrderResponse givenPaidOrder() throws Exception
   {
        OrderResponse orderResponse = givenPaymentPendingOrder();
        updateStatus(orderResponse.orderNumber(), OrderStatus.PAID, "update-order-status-contract-002");
        return getOrder(orderResponse.orderNumber());
   }

   // Update order status by order number and idempotency key
    private void updateStatus( String orderNumber,OrderStatus status, String idempotencyKey) {
    
        HttpHeaders headers = jsonHeaders(idempotencyKey);
        String json = """
            {
              "status": "%s"
            }
            """.formatted(status.name());

        ResponseEntity<OrderResponse> response =
            restTemplate.withBasicAuth("admin", "admin")
                    .exchange(
                            "/api/orders/{orderNumber}/status",
                            HttpMethod.PATCH,
                            new HttpEntity<>(json, headers),
                            OrderResponse.class,
                            orderNumber
                    );
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
     }

    // Set JSON headers with idempotency key
    private HttpHeaders jsonHeaders(String idempotencyKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("idempotencyKey", idempotencyKey);
        return headers;
    }

    // Get order by order number
    private OrderResponse getOrder(String orderNumber) {
        ResponseEntity<OrderResponse> response =
                restTemplate.withBasicAuth("admin", "admin")
                        .getForEntity("/api/orders/{orderNumber}", OrderResponse.class, orderNumber);
                        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        return response.getBody();
    }
    
}

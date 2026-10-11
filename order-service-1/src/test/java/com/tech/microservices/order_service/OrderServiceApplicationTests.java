package com.tech.microservices.order_service;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.tech.microservices.order_service.config.SecurityConfig;
import com.tech.microservices.order_service.dto.*;
import com.tech.microservices.order_service.model.Order;
import com.tech.microservices.order_service.repository.IdempotencyKeysRepository;
import com.tech.microservices.order_service.repository.OrderRepository;
import com.tech.microservices.order_service.repository.OutboxEventRepository;
import jakarta.validation.Valid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({SecurityConfig.class, TestcontainersConfiguration.class})
@AutoConfigureTestRestTemplate
@AutoConfigureStubRunner(
	    ids = "com.tech.microservices:inventory-service:+:stubs:8082",
        stubsMode = StubRunnerProperties.StubsMode.LOCAL)
class OrderServiceApplicationTests {

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private IdempotencyKeysRepository idempotencyKeysRepository;

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private OutboxEventRepository outboxEventRepository;

	private OrderRequest orderRequest;

	private List<OrderItemRequest> orderItemRequest;

	private UpdateOrderStatusRequest updateOrderStatusRequest;

	private UpdateOrderBasicRequest updateOrderBasicRequest;

	private String json;;

	@BeforeEach
	void setUp() throws Exception
	{
		outboxEventRepository.deleteAllInBatch();
        idempotencyKeysRepository.deleteAllInBatch();
        orderRepository.deleteAllInBatch();
		InputStream inputeStream =
				getClass().getResourceAsStream("/create-order-request.json");
		assertThat(inputeStream).isNotNull();
		json = new String(inputeStream.readAllBytes(), StandardCharsets.UTF_8);
        orderRequest = objectMapper.readValue(json, OrderRequest.class);
		orderItemRequest = orderRequest.orderItems();

	   String newStatusString = new String(getClass().getResourceAsStream("/update-order-status.json").readAllBytes(), StandardCharsets.UTF_8);
		updateOrderStatusRequest = objectMapper.readValue(newStatusString, UpdateOrderStatusRequest.class);

		String newBasicString = new String(getClass().getResourceAsStream("/update-order-basic-info.json").readAllBytes(), StandardCharsets.UTF_8);
		updateOrderBasicRequest = objectMapper.readValue(newBasicString, UpdateOrderBasicRequest.class);
	}

	@Test
	void shouldCreateANewOrder() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("idempotencyKey", UUID.randomUUID().toString());
		HttpEntity<OrderRequest> request = new HttpEntity<>(orderRequest, headers);

		ResponseEntity<Void> response = this.restTemplate
				//.withBasicAuth("user","123")  // SecurityConfig.class settled the role is amdin, username is user, password is 123
				.withBasicAuth("admin","admin")  // admin is the role of the ADMIN user
				.postForEntity("/api/orders", request, void.class); // OrderService.class createOrder() asks to transfer an idempotencyKey
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(orderRepository.count()).isEqualTo(1);
		//assertThat(orderItemsRepository.count()).isEqualTo(1);
		assertThat(idempotencyKeysRepository.count()).isEqualTo(1);
		assertThat(outboxEventRepository.count()).isEqualTo(1);


		System.out.println("Status code is null or not?" + response.getStatusCode());
		System.out.println("orderRepository.count()" + orderRepository.count());
		System.out.println("idempotencyKeysRepository.count()" + idempotencyKeysRepository.count());
		System.out.println("outboxEventRepository.count()" + outboxEventRepository.count());


		URI location = response.getHeaders().getLocation();
		ResponseEntity<String> getResponse = this.restTemplate
				.withBasicAuth("admin","admin")
				.getForEntity(location, String.class);
		assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(getResponse.getStatusCode()).as("Status code is null or not?" + getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        System.out.println("Status code is null or not?" + getResponse.getStatusCode());
		System.out.println("Response is " + getResponse.toString());
	}

	// Update Order status
	// 1 step : create new order
	// 2 step : update order status
	// 3 step : get the order status and make sure it changed
	@Test
	void shouldUpdateOrderStatus() {
		//create header and request
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("idempotencyKey", UUID.randomUUID().toString());
		HttpEntity<OrderRequest> request = new HttpEntity<>(orderRequest, headers);

		//create order
		ResponseEntity<Void> response = this.restTemplate
				.withBasicAuth("admin","admin")  // SecurityConfig.class settled the role is amdin, username is admin, password is admin
				.postForEntity("/api/orders", request, void.class); // OrderService.class createOrder() asks to transfer an idempotencyKey
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		System.out.println("shouldUpdateOrderStatus()after created order method and the code is " + response.getStatusCode());


		//create update header and request
		HttpHeaders updateHeaders = new HttpHeaders();
		updateHeaders.setContentType(MediaType.APPLICATION_JSON);
		updateHeaders.set("idempotencyKey", UUID.randomUUID().toString());
		HttpEntity<UpdateOrderStatusRequest> updateRequest = new HttpEntity<>(updateOrderStatusRequest, updateHeaders);

		// update order status
		//这里一定记住 修改部分或者全部都要用exchange 不要用put, patch,因为拿不到状态码，比如 200，204
		ResponseEntity<Void> updateResponse = this.restTemplate
				.withBasicAuth("admin","admin") // use admin role and admin account to update order status
				//.withBasicAuth("user","123")  // SecurityConfig.class settled the role is user, username is user, password is 123
				.exchange(
						"/api/orders/{orderNumber}/status",
						HttpMethod.PATCH,
						updateRequest,
						Void.class,
						orderRequest.orderNumber()
				);
		assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		System.out.println("shouldUpdateOrderStatus() after updated order and the Status code is " + updateResponse.getStatusCode());

		//create order status changed event
		//create header and request
		Optional<Order> order = orderRepository.findByOrderNumber(orderRequest.orderNumber());
		Order savedOrder = order.orElseThrow();
		assertThat(savedOrder.getOrderStatus()).isEqualTo(updateOrderStatusRequest.status());
		System.out.println("shouldUpdateOrderStatus() after updated order and get the order again, the order status code is " + savedOrder.getOrderStatus());

	}

	@Test
	void shouldCancelOrder() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("idempotencyKey", UUID.randomUUID().toString());
		HttpEntity<OrderRequest> request = new HttpEntity<>(orderRequest, headers);

		//create order
		ResponseEntity<Void> response = this.restTemplate
				.withBasicAuth("admin","admin")  // SecurityConfig.class settled the role is amdin, username is admin, password is admin
				.postForEntity("/api/orders", request, void.class); // OrderService.class createOrder() asks to transfer an idempotencyKey
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		System.out.println("shouldCancelOrder()after created order method and the code is " + response.getStatusCode());

		//create cancel header and request
		HttpHeaders cancelHeaders = new HttpHeaders();
		cancelHeaders.setContentType(MediaType.APPLICATION_JSON);
		cancelHeaders.set("idempotencyKey", UUID.randomUUID().toString());
		HttpEntity<Void> cancelRequest = new HttpEntity<>(cancelHeaders);

		//cancel order
		ResponseEntity<Void> cancleResponse = this.restTemplate
				.withBasicAuth("admin","admin")  // SecurityConfig.class settled the role is amdin, username is admin, password is admin
				.exchange(
						"/api/orders/{orderNumber}/cancel",
						HttpMethod.PATCH,
						cancelRequest,
						Void.class,
						orderRequest.orderNumber()
				);
		assertThat(cancleResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		System.out.println("shouldCancelOrder()after cancel method and the code is " + cancleResponse.getStatusCode());

		Optional<Order> order = orderRepository.findByOrderNumber(orderRequest.orderNumber());
		Order savedOrder = order.orElseThrow();
		assertThat(savedOrder.getOrderStatus()).isEqualTo(updateOrderStatusRequest.status());
		System.out.println("shouldCancelOrder() after updated order and get the order again, the order status code is " + savedOrder.getOrderStatus());

	}


	@Test
	void shouldUpdateOrderBasic() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("idempotencyKey", UUID.randomUUID().toString());
		HttpEntity<OrderRequest> request = new HttpEntity<>(orderRequest, headers);

		//create order
		ResponseEntity<Void> response = this.restTemplate
				.withBasicAuth("admin","admin")  // SecurityConfig.class settled the role is amdin, username is admin, password is admin
				.postForEntity("/api/orders", request, void.class); // OrderService.class createOrder() asks to transfer an idempotencyKey
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		System.out.println("shouldUpdateOrderBasic()after update order method and the code is " + response.getStatusCode());
		System.out.println("shouldUpdateOrderBasic()before update order method and billingAddress is "+ request.getBody().billingAddress());
		System.out.println("shouldUpdateOrderBasic()before update order method and shippingAddress is "+ request.getBody().shippingAddress());

		//create update header and request
		HttpHeaders updateHeaders = new HttpHeaders();
		updateHeaders.setContentType(MediaType.APPLICATION_JSON);
		updateHeaders.set("idempotencyKey", UUID.randomUUID().toString());
		HttpEntity<UpdateOrderBasicRequest> updateRequest = new HttpEntity<>(updateOrderBasicRequest, updateHeaders);

		ResponseEntity<Void> updateResponse = this.restTemplate
				.withBasicAuth("admin","admin")  // SecurityConfig.class settled the role is amdin, username is admin, password is admin
				.exchange(
						"/api/orders/{orderNumber}/basic",
						HttpMethod.PATCH,
						updateRequest,
						Void.class,
						orderRequest.orderNumber()
				);
		assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		System.out.println("shouldUpdateOrderBasic()after cancel method and the code is " + updateResponse.getStatusCode());

		Optional<Order> order = orderRepository.findByOrderNumber(orderRequest.orderNumber());
		Order savedOrder = order.orElseThrow();
		System.out.println("shouldUpdateOrderBasic() after updated order:  " + savedOrder.getBillingAddress());
		System.out.println("shouldUpdateOrderBasic() after updated order:  " + savedOrder.getShippingAddress());
		assertThat(savedOrder.getBillingAddress()).isEqualTo(updateOrderBasicRequest.billingAddress());
		assertThat(savedOrder.getShippingAddress()).isEqualTo(updateOrderBasicRequest.shippingAddress());
		System.out.println("shouldUpdateOrderBasic() after updated order:  " + savedOrder.getRecipientName());
		System.out.println("shouldUpdateOrderBasic() after updated order:  " + savedOrder.getRecipientPhone());
	}


}

package com.tech.microservices.inventory_service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import com.tech.microservices.inventory_service.dto.InventoryResponse;
import com.tech.microservices.inventory_service.dto.InventoryTransactionResponse;
import com.tech.microservices.inventory_service.model.TransactionType;
import com.tech.microservices.inventory_service.model.OutboxEvents;
import com.tech.microservices.inventory_service.model.ReservationStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.tech.microservices.inventory_service.dto.InventoryConfirmRequest;
import com.tech.microservices.inventory_service.dto.InventoryReleaseRequest;
import com.tech.microservices.inventory_service.dto.InventoryRequest;
import com.tech.microservices.inventory_service.dto.InventoryReservationExpiredRequest;
import com.tech.microservices.inventory_service.dto.InventoryReservationRequest;
import com.tech.microservices.inventory_service.dto.InventoryReservationResponse;
import com.tech.microservices.inventory_service.dto.StockInRequest;
import com.tech.microservices.inventory_service.model.IdempotencyKeyStatus;
import com.tech.microservices.inventory_service.model.IdempotencyKeys;
import com.tech.microservices.inventory_service.model.InventoryReservation;
import com.tech.microservices.inventory_service.model.InventoryTransaction;

import static org.assertj.core.api.Assertions.assertThat;
import com.tech.microservices.inventory_service.repository.IdempotencyKeyRepository;
import com.tech.microservices.inventory_service.repository.InventoryRepository;
import com.tech.microservices.inventory_service.repository.InventoryReservationRepository;
import com.tech.microservices.inventory_service.repository.InventoryTransactionRepository;
import com.tech.microservices.inventory_service.repository.OutboxEventRepository;
import com.tech.microservices.inventory_service.service.InventoryReservationService;
import com.tech.microservices.inventory_service.service.InventoryService;
import com.tech.microservices.inventory_service.service.InventoryTransactionService;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;
//import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Slf4j
//@AutoConfigureTestRestTemplate
public class InventoryServiceIntergrationTest {

    @Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private InventoryTransactionRepository inventoryTransactionRepository;

	@Autowired
	private InventoryReservationRepository inventoryReservationRepository;

	@Autowired
	private InventoryTransactionService inventoryTransactionService;

	@Autowired
	private InventoryRepository inventoryRepository;

	@Autowired
	private InventoryReservationService inventoryReservationService;

    @Autowired
	private IdempotencyKeyRepository idempotencyKeysRepository;

	@Autowired
	private InventoryService inventoryService;


	@Autowired
	private OutboxEventRepository outboxEventRepository;

    private String json;;

    private InventoryRequest createRequest;
    private StockInRequest stockInRequest;
    private InventoryReservationRequest reserveRequest;

	/* 为了使数据库中的数据保持一致性，测试用例的执行顺序必须按照以下顺序进行：
	* 1. testCreateInventory() - 创建库存记录
	* 2. testStockIn() - 入库操作
	* 3. testReserveInventory() - 预留库存操作
	* 4. testConfirmInventory() - 确认库存操作
	* 5. testReleaseInventory() - 释放库存操作
	* 6. testInventoryReservationExpired() - 库存预留过期操作
	* 7. testGetInventoryBySkuId() - 根据SKU ID获取库存记录
	* 8. testGetInventoryBySkuIdAndOrderNo() - 根据SKU ID和订单号获取库存记录
	* 9. testGetInventoryTransactionsBySkuId() - 根据SKU ID获取库存
	* 10. testGetInventoryTransactionsBySkuIdAndOrderNo() - 根据SKU ID和订单号获取库存
	* 
	* *** 为了保证测试数据隔离和验证数据真实写入，每个测试方法执行前 都需要清理数据库中的数据，确保每个测试方法的执行不会受到其他测试方法的影响
	* 删除顺序是：
	* 1 先删除库存事务表（InventoryTransaction），
	* 2 再删除库存预留表（InventoryReservation）
	* 3 最后删除库存表（Inventory）。
	* 这样可以确保在删除库存记录时，不会因为存在关联的库存事务或库存预留而导致外键约束错误。
	*
	*/
    @BeforeEach
	void setUp() throws Exception
	{
		// 清理数据库中的数据，确保每个测试方法的执行不会受到其他测试方法的影响
		inventoryTransactionRepository.deleteAll();
		inventoryReservationRepository.deleteAll();
		idempotencyKeysRepository.deleteAll();
		outboxEventRepository.deleteAll();
		inventoryRepository.deleteAll();
		
	
    // 之前的方法是直接从配置文件读取模拟测试数据 但是有个问题就是数据隔离性不够好，可能会导致测试数据被污染，所以改为每次测试都从随机生成的skuId和orderNo来创建测试数据，这样可以确保每个测试方法的数据都是独立的，不会相互影响。
	// 	InputStream inputeStream =
	// 			getClass().getResourceAsStream("/create-inventory-request.json");
	// 	assertThat(inputeStream).isNotNull();
	// 	json = new String(inputeStream.readAllBytes(), StandardCharsets.UTF_8);
    //     createRequest = objectMapper.readValue(json, InventoryRequest.class);

	//    String stockIn = new String(getClass().getResourceAsStream("/stock-in-request.json").readAllBytes(), StandardCharsets.UTF_8);
	//    assertThat(stockIn).isNotNull();
    //    stockInRequest = objectMapper.readValue(stockIn, StockInRequest.class);
    //    assertThat(stockInRequest).isNotNull();
	// 	String reserve = new String(getClass().getResourceAsStream("/reserve-inventory-request.json").readAllBytes(), StandardCharsets.UTF_8);
	// 	assertThat(reserve).isNotNull();
	// 	reserveRequest = objectMapper.readValue(reserve, InventoryReservationRequest.class);
	// 	assertThat(reserveRequest).isNotNull();
	// 	imdempotencyKey = UUID.randomUUID().toString();
	}

	private InventoryResponse givenInventory(String skuId, Long onHandQuantity)
	{
       return inventoryService.createInventory(newKey(), new InventoryRequest(skuId, onHandQuantity)); 
	}

	private InventoryResponse givenReservation(String skuId, String orderNo, Long quantity)
	{
		givenInventory(skuId, 100L);
		return inventoryService.reserveInventory(newKey(), new InventoryReservationRequest(orderNo, skuId, quantity));
	}


	// For SkuId, 
	// we will generate a new one for each test case to ensure data isolation 
	// and verify that the data is actually written
	private String newSkuId()
	{
		return "sku-" + UUID.randomUUID().toString();
	}

	// For OrderNo, 
	// we will generate a new one for each test case to ensure data isolation
	// and verify that the data is actually written
	private String newOrderNo()
	{
		return "order-" + UUID.randomUUID().toString();
	}

	// For IdempotencyKey,
	// we will generate a new one for each test case to ensure data isolation
	// and verify that the data is actually written
	private String newKey()
	{
		return "key-" + UUID.randomUUID().toString();
	}


	@Test
	public void testCreateInventory() throws Exception
	{
		InventoryResponse response = this.givenInventory(newSkuId(), 100L);
		assertThat(response).isNotNull();
		assertThat(response.skuId()).isNotNull();
		assertThat(response.onHandQuantity()).isEqualTo(100);
		assertThat(response.reservedQuantity()).isEqualTo(0);
		assertThat(response.availableQuantity()).isEqualTo(100);
		log.info("Create Inventory Response: {}", response.toString());
	}

	@Test
	public void testStockIn() throws Exception
	{
		InventoryResponse response = this.givenInventory(newSkuId(), 100L);
		InventoryResponse stockInResponse = this.inventoryService.stockIn(newKey(), new StockInRequest(response.skuId(), 1000L));
		log.info("Stock In Response: {}", stockInResponse.toString());
		log.info("Stock In before quantity: {}", response.onHandQuantity());
		log.info("Stock in after the quantity:{} ",stockInResponse.onHandQuantity());
		assertThat(stockInResponse).isNotNull();
		assertThat(stockInResponse.onHandQuantity()).isEqualTo(1100);
		assertThat(stockInResponse.reservedQuantity()).isEqualTo(0);
		assertThat(stockInResponse.availableQuantity()).isEqualTo(1100);
		assertThat(stockInResponse.skuId()).isEqualTo(response.skuId());
		assertThat(stockInResponse.onHandQuantity()).isNotEqualTo(response.onHandQuantity());
		
	}


	@Test
	public void testReserveInventory() throws Exception
	{
		String skuId = newSkuId();
		String orderNo = newOrderNo();
		String reserveKey = newKey();
		InventoryResponse response = this.givenInventory(skuId, 100L);
		InventoryResponse reserveResponse = this.inventoryService.reserveInventory(reserveKey, new InventoryReservationRequest(orderNo, skuId, 50L));
		log.info("Reserve Inventory Response: {}", reserveResponse.toString());
		log.info("Reserve Inventory before reserved quantity: {}", response.reservedQuantity());
		log.info("Reserve Inventory after reserved quantity: {}", reserveResponse.reservedQuantity());
		log.info("Reserve Inventory before available quantity: {}", response.availableQuantity());
		log.info("Reserve Inventory after available quantity: {}", reserveResponse.availableQuantity());
		assertThat(reserveResponse).isNotNull();
		assertThat(reserveResponse.onHandQuantity()).isEqualTo(100);
		assertThat(reserveResponse.reservedQuantity()).isEqualTo(50);
		assertThat(reserveResponse.availableQuantity()).isEqualTo(50);
		assertThat(reserveResponse.skuId()).isEqualTo(response.skuId());

        InventoryReservation reservation = this.inventoryReservationRepository.findBySkuIdAndOrderNo(skuId, orderNo).orElseThrow();
		log.info("Reservation response is{}",reservation.toString());
		log.info("Reservation status is {}",reservation.getStatus());
		log.info("Reservation quantity is {}",reservation.getQuantity());
		assertThat(reservation.getReservationNo()).isNotNull();
		assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RESERVED);
		assertThat(reservation.getQuantity()).isEqualTo(50L);

		Page<InventoryTransaction> transactionPage = this.inventoryTransactionRepository.findBySkuIdAndOrderNo(skuId, orderNo, PageRequest.of(0, 10));
		
		log.info("Transaction page content is: {}", transactionPage.getContent());
		log.info("Transaction page size is: {}", transactionPage.getSize());
		log.info("Transaction page total elements is: {}", transactionPage.getTotalElements());
		log.info("Transaction page total pages is: {}", transactionPage.getTotalPages());
		log.info("Transaction page number 0 transaction type is: {}", transactionPage.getContent().get(0).getTransactionType());
		log.info("Transaction page number 0 transaction quantity is: {}", transactionPage.getContent().get(0).getQuantity());
		assertThat(transactionPage.getContent()).hasSize(1);
		assertThat(transactionPage.getContent().get(0).getTransactionType()).isEqualTo(TransactionType.RESERVE);
		assertThat(transactionPage.getContent().get(0).getQuantity()).isEqualTo(50L) ;

		List<OutboxEvents> outboxEvents = this.outboxEventRepository.findAll();
		log.info("OutboxEvents size is: {}", outboxEvents.size());
		log.info("OutboxEvents content is: {}", outboxEvents);
		log.info("OutboxEvents second event aggregateId is: {}", outboxEvents.get(1).getAggregateType());
		log.info("OutboxEvents second event eventType is: {}", outboxEvents.get(1).getEventType());
		log.info("OutboxEvents second event status is: {}", outboxEvents.get(1).getStatus());
		assertThat(this.outboxEventRepository.findAll()).anyMatch(event -> event.getAggregateId().equals(skuId) && event.getEventType().equals("INVENTORY_RESERVED") && event.getStatus().equals("PENDING"));

		IdempotencyKeys idempotencyKey = this.idempotencyKeysRepository.findByIdempotencyKey(reserveKey).orElseThrow();
		log.info("IdempotencyKey is: {}", idempotencyKey);
		log.info("IdempotencyKey status is: {}", idempotencyKey.getStatus());
		log.info("IdempotencyKey responseStatus is: {}", idempotencyKey.getResponseStatus());
		log.info("IdempotencyKey responseBody is: {}", idempotencyKey.getResponseBody());
		assertThat(idempotencyKey.getStatus()).isEqualTo(IdempotencyKeyStatus.COMPLETED.name());
        assertThat(idempotencyKey.getResponseStatus()).isEqualTo(200);
        assertThat(idempotencyKey.getResponseBody()).isNotBlank();

	}


	@Test
	public void testConfirmInventory() throws Exception
	{
		String skuId = newSkuId();
		String orderNo = newOrderNo();
		String reserveKey = newKey();

		InventoryResponse reserveResponse = this.givenReservation(skuId, orderNo, 50L);
		InventoryResponse confirmResponse = this.inventoryService.confirmReservation(reserveKey, new InventoryConfirmRequest(orderNo, skuId, 50L));
		
		log.info("Reserve Inventory confirmed Response: {}", confirmResponse.toString());
		log.info("Reserve Inventory before confirmed reserved quantity: {}", reserveResponse.reservedQuantity());
		log.info("Reserve Inventory after confirmed reserved quantity: {}", confirmResponse.reservedQuantity());
		log.info("Reserve Inventory before confirmed available quantity: {}", reserveResponse.availableQuantity());
		log.info("Reserve Inventory after confirmed available quantity: {}", confirmResponse.availableQuantity());
		log.info("Reserve Inventory before confirmed onHand quantity: {}", reserveResponse.onHandQuantity());
		log.info("Reserve Inventory after confirmed onHand quantity: {}", confirmResponse.onHandQuantity());
		assertThat(reserveResponse).isNotNull();
		assertThat(reserveResponse.onHandQuantity()).isEqualTo(100);
		assertThat(reserveResponse.reservedQuantity()).isEqualTo(50);
		assertThat(reserveResponse.availableQuantity()).isEqualTo(50);
		assertThat(confirmResponse.skuId()).isEqualTo(reserveResponse.skuId());
		assertThat(confirmResponse.onHandQuantity()).isEqualTo(50L);
        assertThat(confirmResponse.reservedQuantity()).isEqualTo(0L);
        assertThat(confirmResponse.availableQuantity()).isEqualTo(50L);

		InventoryReservation reservation = this.inventoryReservationRepository.findBySkuIdAndOrderNo(skuId, orderNo).orElseThrow();
		log.info("Reservation response is{}",reservation.toString());
		log.info("Reservation status is {}",reservation.getStatus());
		log.info("Reservation quantity is {}",reservation.getQuantity());
		assertThat(reservation.getReservationNo()).isNotNull();
		assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
		assertThat(reservation.getQuantity()).isEqualTo(50L);

		Page<InventoryTransaction> transactionPage = this.inventoryTransactionRepository.findBySkuIdAndOrderNo(skuId, orderNo, PageRequest.of(0, 10));
		
		log.info("Transaction page content is: {}", transactionPage.getContent());
		log.info("Transaction page size is: {}", transactionPage.getSize());
		log.info("Transaction page total elements is: {}", transactionPage.getTotalElements());
		log.info("Transaction page total pages is: {}", transactionPage.getTotalPages());
		log.info("Transaction page number 1 transaction type is: {}", transactionPage.getContent().get(1).getTransactionType());
		log.info("Transaction page number 1 transaction quantity is: {}", transactionPage.getContent().get(1).getQuantity());
		assertThat(transactionPage.getContent()).hasSize(2);
		assertThat(transactionPage.getContent().get(0).getTransactionType()).isEqualTo(TransactionType.RESERVE);
		assertThat(transactionPage.getContent().get(1).getTransactionType()).isEqualTo(TransactionType.SALE);
		assertThat(transactionPage.getContent().get(1).getQuantity()).isEqualTo(50L) ;

		List<OutboxEvents> outboxEvents = this.outboxEventRepository.findAll();
		log.info("OutboxEvents size is: {}", outboxEvents.size());
		log.info("OutboxEvents content is: {}", outboxEvents);
		log.info("OutboxEvents second event aggregateId is: {}", outboxEvents.get(1).getAggregateType());
		log.info("OutboxEvents second event eventType is: {}", outboxEvents.get(1).getEventType());
		log.info("OutboxEvents second event status is: {}", outboxEvents.get(1).getStatus());
		assertThat(this.outboxEventRepository.findAll()).anyMatch(event -> event.getAggregateId().equals(skuId) && event.getEventType().equals("INVENTORY_RESERVATION_CONFIRMED") && event.getStatus().equals("PENDING"));

		IdempotencyKeys idempotencyKey = this.idempotencyKeysRepository.findByIdempotencyKey(reserveKey).orElseThrow();
		log.info("IdempotencyKey is: {}", idempotencyKey);
		log.info("IdempotencyKey status is: {}", idempotencyKey.getStatus());
		log.info("IdempotencyKey responseStatus is: {}", idempotencyKey.getResponseStatus());
		log.info("IdempotencyKey responseBody is: {}", idempotencyKey.getResponseBody());
		assertThat(idempotencyKey.getStatus()).isEqualTo(IdempotencyKeyStatus.COMPLETED.name());
        assertThat(idempotencyKey.getResponseStatus()).isEqualTo(200);
        assertThat(idempotencyKey.getResponseBody()).isNotBlank();
	}


	@Test
	public void testReleaseInventory() throws Exception
	{
		String skuId = newSkuId();
		String orderNo = newOrderNo();
		String reserveKey = newKey();

		InventoryResponse reserveResponse = this.givenReservation(skuId, orderNo, 50L);
		InventoryResponse releaseResponse = this.inventoryService.releaseInventory(reserveKey, new InventoryReleaseRequest(orderNo, skuId, 50L));
		
		log.info("Reserve Inventory released Response: {}", releaseResponse.toString());
		log.info("Reserve Inventory before released reserved quantity: {}", reserveResponse.reservedQuantity());
		log.info("Reserve Inventory after released reserved quantity: {}", releaseResponse.reservedQuantity());
		log.info("Reserve Inventory before released available quantity: {}", reserveResponse.availableQuantity());
		log.info("Reserve Inventory after released available quantity: {}", releaseResponse.availableQuantity());
		log.info("Reserve Inventory before released onHand quantity: {}", reserveResponse.onHandQuantity());
		log.info("Reserve Inventory after released onHand quantity: {}", releaseResponse.onHandQuantity());
		assertThat(reserveResponse).isNotNull();
		assertThat(reserveResponse.onHandQuantity()).isEqualTo(100);
		assertThat(reserveResponse.reservedQuantity()).isEqualTo(50);
		assertThat(reserveResponse.availableQuantity()).isEqualTo(50);
		assertThat(releaseResponse.skuId()).isEqualTo(reserveResponse.skuId());
		assertThat(releaseResponse.onHandQuantity()).isEqualTo(100L);
        assertThat(releaseResponse.reservedQuantity()).isEqualTo(0L);
        assertThat(releaseResponse.availableQuantity()).isEqualTo(100L);

		InventoryReservation reservation = this.inventoryReservationRepository.findBySkuIdAndOrderNo(skuId, orderNo).orElseThrow();
		log.info("Reservation response is{}",reservation.toString());
		log.info("Reservation status is {}",reservation.getStatus());
		log.info("Reservation quantity is {}",reservation.getQuantity());
		assertThat(reservation.getReservationNo()).isNotNull();
		assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RELEASED);
		assertThat(reservation.getQuantity()).isEqualTo(50L);

		Page<InventoryTransaction> transactionPage = this.inventoryTransactionRepository.findBySkuIdAndOrderNo(skuId, orderNo, PageRequest.of(0, 10));
		
		log.info("Transaction page content is: {}", transactionPage.getContent());
		log.info("Transaction page size is: {}", transactionPage.getSize());
		log.info("Transaction page total elements is: {}", transactionPage.getTotalElements());
		log.info("Transaction page total pages is: {}", transactionPage.getTotalPages());
		log.info("Transaction page number 1 transaction type is: {}", transactionPage.getContent().get(0).getTransactionType());
		log.info("Transaction page number 1 transaction quantity is: {}", transactionPage.getContent().get(0).getQuantity());
		log.info("Transaction page number 2 transaction type is: {}", transactionPage.getContent().get(1).getTransactionType());
		log.info("Transaction page number 2 transaction quantity is: {}", transactionPage.getContent().get(1).getQuantity());
		assertThat(transactionPage.getContent()).hasSize(2);
		assertThat(transactionPage.getContent().get(0).getTransactionType()).isEqualTo(TransactionType.RESERVE);
		assertThat(transactionPage.getContent().get(0).getQuantity()).isEqualTo(50L);
		assertThat(transactionPage.getContent().get(1).getTransactionType()).isEqualTo(TransactionType.RELEASE);
		assertThat(transactionPage.getContent().get(1).getQuantity()).isEqualTo(50L) ;

		List<OutboxEvents> outboxEvents = this.outboxEventRepository.findAll();
		log.info("OutboxEvents size is: {}", outboxEvents.size());
		log.info("OutboxEvents content is: {}", outboxEvents);
		log.info("OutboxEvents second event aggregateId is: {}", outboxEvents.get(1).getAggregateType());
		log.info("OutboxEvents second event eventType is: {}", outboxEvents.get(1).getEventType());
		log.info("OutboxEvents second event status is: {}", outboxEvents.get(1).getStatus());
		assertThat(this.outboxEventRepository.findAll()).anyMatch(event -> event.getAggregateId().equals(skuId) && event.getEventType().equals("INVENTORY_RESERVATION_RELEASED") && event.getStatus().equals("PENDING"));

		IdempotencyKeys idempotencyKey = this.idempotencyKeysRepository.findByIdempotencyKey(reserveKey).orElseThrow();
		log.info("IdempotencyKey is: {}", idempotencyKey);
		log.info("IdempotencyKey status is: {}", idempotencyKey.getStatus());
		log.info("IdempotencyKey responseStatus is: {}", idempotencyKey.getResponseStatus());
		log.info("IdempotencyKey responseBody is: {}", idempotencyKey.getResponseBody());
		assertThat(idempotencyKey.getStatus()).isEqualTo(IdempotencyKeyStatus.COMPLETED.name());
        assertThat(idempotencyKey.getResponseStatus()).isEqualTo(200);
        assertThat(idempotencyKey.getResponseBody()).isNotBlank();
	}



	@Test
	public void testInventoryReservationExpired() throws Exception
	{

		
		String skuId = newSkuId();
		String orderNo = newOrderNo();

		InventoryResponse reserveResponse = this.givenReservation(skuId, orderNo, 50L);

		InventoryReservation reservation =
        inventoryReservationRepository.findBySkuIdAndOrderNo(skuId, orderNo)
                .orElseThrow();
       reservation.setExpiresAt(LocalDateTime.now().minusMinutes(1));
       inventoryReservationRepository.save(reservation);

		
		InventoryResponse expiredResponse = this.inventoryService.expireReservation(newKey(), 
		new InventoryReservationExpiredRequest(orderNo, skuId, 50L));
		
		log.info("Reserve Inventory expired Response: {}", expiredResponse.toString());
		log.info("Reserve Inventory before expired reserved quantity: {}", reserveResponse.reservedQuantity());
		log.info("Reserve Inventory after expired reserved quantity: {}", expiredResponse.reservedQuantity());
		log.info("Reserve Inventory before expired available quantity: {}", reserveResponse.availableQuantity());
		log.info("Reserve Inventory after expired available quantity: {}", expiredResponse.availableQuantity());
		log.info("Reserve Inventory before expired onHand quantity: {}", reserveResponse.onHandQuantity());
		log.info("Reserve Inventory after expired onHand quantity: {}", expiredResponse.onHandQuantity());
		assertThat(reserveResponse).isNotNull();
		assertThat(reserveResponse.onHandQuantity()).isEqualTo(100L);
		assertThat(reserveResponse.reservedQuantity()).isEqualTo(50L);
		assertThat(reserveResponse.availableQuantity()).isEqualTo(50L);
		assertThat(expiredResponse.skuId()).isEqualTo(reserveResponse.skuId());
		assertThat(expiredResponse.onHandQuantity()).isEqualTo(100L);
        assertThat(expiredResponse.reservedQuantity()).isEqualTo(0L);
        assertThat(expiredResponse.availableQuantity()).isEqualTo(100L);
	}


	@Test
	public void testGetInventoryBySkuId() throws Exception
	{
		String skuId = newSkuId();
		this.givenInventory(skuId, 100L);
		InventoryResponse response = inventoryService.getInventoryBySkuId(skuId);
		log.info("Get Inventory By SkuId Response: {}", response.toString());
		assertThat(response).isNotNull();
		assertThat(response.skuId()).isEqualTo(skuId);
		assertThat(response.onHandQuantity()).isEqualTo(100L);
		assertThat(response.reservedQuantity()).isEqualTo(0L);
		assertThat(response.availableQuantity()).isEqualTo(100L);
	}


	@Test
	public void testGetInventoryBySkuIdAndOrderNo() throws Exception
	{
		String skuId = newSkuId();
		String orderNo = newOrderNo();
		this.givenReservation(skuId, orderNo, 50L);
		InventoryResponse response = inventoryService.getInventoryBySkuIdAndOrderNo(skuId, orderNo);
		log.info("Get Inventory By SkuId and OrderNo Response: {}", response.toString());
		assertThat(response).isNotNull();
		assertThat(response.skuId()).isEqualTo(skuId);
		assertThat(response.onHandQuantity()).isEqualTo(100L);
		assertThat(response.reservedQuantity()).isEqualTo(50L);
		assertThat(response.availableQuantity()).isEqualTo(50L);
	}


	@Test
	public void testGetInventoryTransactionsBySkuId() throws Exception
	{
		String skuId = newSkuId();
		String orderNo = newOrderNo();
		this.givenReservation(skuId, orderNo, 50L);
		Pageable pageable = PageRequest.of(0,10);
		Page<InventoryTransactionResponse> response = inventoryService.getAllTransaction(skuId, pageable);
		log.info("Get Inventory Transactions By SkuId Response: {}", response.toString());
		assertThat(response).isNotNull();
		assertThat(response.getContent().size()).isGreaterThan(0);
		assertThat(response.getTotalElements()).isEqualTo(2L);
		assertThat(response.getContent().get(0).skuId()).isEqualTo(skuId);
		assertThat(response.getContent().get(0).transactionType()).isEqualTo(TransactionType.INITIAL_STOCK);
		assertThat(response.getContent().get(1).skuId()).isEqualTo(skuId);
		assertThat(response.getContent().get(1).transactionType()).isEqualTo(TransactionType.RESERVE);
		assertThat(response.getContent().get(1).orderNo()).isEqualTo(orderNo);
		assertThat(response.getContent().get(1).quantity()).isEqualTo(50L);
		assertThat(response.getContent())
        .extracting(InventoryTransactionResponse::transactionType)
        .contains(TransactionType.INITIAL_STOCK, TransactionType.RESERVE);
	}


	//在写两个reservation + transation  的集合测试就搞定了

	@Test
	public void testGetInventoryTransactionsByOrderNo() throws Exception
	{
		String skuId = newSkuId();
		String orderNo = newOrderNo();
		this.givenReservation(skuId, orderNo, 50L);
		Pageable pageable = PageRequest.of(0,10);
		Page<InventoryTransactionResponse> response = this.inventoryTransactionService.getAllTransactionByOrderNo(orderNo, pageable);
		log.info("Get Inventory Transactions By SkuId and OrderNo Response: {}", response.toString());
		assertThat(response).isNotNull();
		assertThat(response.getContent().size()).isGreaterThan(0);
	}

	@Test
	public void testGetInventoryReservationsByOrderNo() throws Exception
	{
		String skuId = newSkuId();
		String orderNo = newOrderNo();
		this.givenReservation(skuId, orderNo, 50L);
		Pageable pageable = PageRequest.of(0,10);
		Page<InventoryReservationResponse> response = this.inventoryReservationService.getAllReservationByOrderNo(orderNo, pageable);
		
		log.info("Get Inventory Reservations By SkuId and OrderNo Response: {}", response.toString());
		assertThat(response).isNotNull();
		assertThat(response.getContent().size()).isGreaterThan(0);
	}

	@Test
	public void testGetAllInventory() throws Exception
	{
		String skuId1 = newSkuId();
		String skuId2 = newSkuId();
		this.givenInventory(skuId1, 100L);
		this.givenInventory(skuId2, 200L);
		Pageable pageable = PageRequest.of(0,10);
		Page<InventoryResponse> response = inventoryService.getAllInventory(pageable);
		log.info("Get All Inventory Response: {}", response.toString());
		assertThat(response).isNotNull();
		assertThat(response.getContent().size()).isGreaterThan(0);
		assertThat(response.getTotalElements()).isEqualTo(2L);
	}

	@Test
	public void testGetAllReservations() throws Exception
	{
		String skuId1 = newSkuId();
		String skuId2 = newSkuId();
		String orderNo1 = newOrderNo();
		String orderNo2 = newOrderNo();
		this.givenReservation(skuId1, orderNo1, 50L);
		this.givenReservation(skuId2, orderNo2, 100L);
		Pageable pageable = PageRequest.of(0,10);
		Page<InventoryReservationResponse> response = this.inventoryReservationService.getAllReservation(pageable);
		log.info("Get All Reservations Response: {}", response.toString());
		assertThat(response).isNotNull();
		assertThat(response.getContent().size()).isGreaterThan(0);
		assertThat(response.getTotalElements()).isEqualTo(2L);
	}

	@Test
	public void testGetAllTransactions() throws Exception
	{
		String skuId1 = newSkuId();
		String skuId2 = newSkuId();
		String orderNo1 = newOrderNo();
		String orderNo2 = newOrderNo();
		this.givenReservation(skuId1, orderNo1, 50L);
		this.givenReservation(skuId2, orderNo2, 100L);
		Pageable pageable = PageRequest.of(0,10);
		Page<InventoryTransactionResponse> response = this.inventoryTransactionService.getAllTransaction(pageable);
		log.info("Get All Transactions Response: {}", response.toString());
		assertThat(response).isNotNull();
		assertThat(response.getContent().size()).isGreaterThan(0);
		assertThat(response.getTotalElements()).isEqualTo(4L);
	}
}

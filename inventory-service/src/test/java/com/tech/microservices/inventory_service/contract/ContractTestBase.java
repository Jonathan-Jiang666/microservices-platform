package com.tech.microservices.inventory_service.contract;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.tech.microservices.inventory_service.exception.ReservationNotFoundException;
import com.tech.microservices.inventory_service.controller.InventoryController;
import com.tech.microservices.inventory_service.dto.InventoryResponse;
import com.tech.microservices.inventory_service.exception.InsufficientInventoryException;

import com.tech.microservices.inventory_service.service.InventoryService;
import io.restassured.module.mockmvc.RestAssuredMockMvc;


/**
 * Inventory Provider Contract Test 基类。
 * 库存服务端的 Contract 测试基类，使用 Mockito 模拟 InventoryService 的行为。
 *
 * Mockito 的作用：
 * 1. 使用 @MockitoBean 替换真正的 InventoryService。
 * 2. 使用 given(...).willReturn(...) 模拟成功场景。
 * 3. 使用 given(...).willThrow(...) 模拟库存不足等异常场景。
 * 4. 让生成的 Contract 测试只验证 Controller 的 HTTP 契约，
 *    包括 URL、请求头、请求体、状态码和响应体。
 *
 * 优点：
 * - 不需要启动数据库、Kafka 或完整业务依赖。
 * - 测试速度快，返回结果稳定。
 * - 可以精确构造成功和异常场景。
 *
 * 注意：
 * Mockito 只模拟 Java 对象行为，不会发送真实的下游 HTTP 请求，
 * 也不能证明 InventoryService 的真实业务逻辑正确。
 * 
 * Provider 端使用 Mockito 模拟的是 InventoryService 的返回结果，
 * 然后验证 InventoryController 是否按照 Contract 返回正确的 HTTP 响应。
 * 执行过程是：
 * 
 * Spring Cloud Contract 生成测试
 *       ↓
 * 调用 InventoryController
 *      ↓
 * Controller 调用 InventoryService
 *        ↓
 * Mockito 模拟 InventoryService 返回成功结果或抛出异常
 *        ↓
 * 验证 Controller 返回的状态码、Header 和 JSON
 * 
 */
@WebMvcTest(InventoryController.class)
@AutoConfigureMockMvc(addFilters = false)
public abstract class ContractTestBase {

    @Autowired
    protected MockMvc mockMvc;

    // Mockito 创建的 InventoryService 替身，不执行真实库存业务逻辑
    @MockitoBean
    protected InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        // Any common setup for contract tests can be done here
        RestAssuredMockMvc.mockMvc(mockMvc);

        // Sample InventoryResponse for contract tests 
        // and mocking the InventoryService created behavior
        InventoryResponse sampleInventoryResponse = new InventoryResponse(
                "sku-contract-001",
                100L,
                10L,
                90L,
                LocalDateTime.parse("2026-09-15T10:00:00"),
                LocalDateTime.parse("2026-09-15T10:00:00")
        );
    
       // Mocking the behavior of the InventoryService for contract tests
    //    given(inventoryService.reserveInventory(anyString(), any(InventoryReservationRequest.class)))
    //             .willReturn(sampleInventoryResponse);
    
    //   // Mocking the behavior of the InventoryService for insufficient inventory scenario
    //    given(inventoryService.reserveInventory(anyString(), any(InventoryReservationRequest.class)))
    //             .willThrow(new InsufficientInventoryException("Inventory insufficient"));




       // Mocking the reserveInventory method to return a successful response for specific test cases
       // Return successful response
       given(inventoryService.reserveInventory(
                 eq("reserve-order-contract-001-sku-contract-001"),
                 argThat(request -> Long.valueOf(10L).equals(request.reservedQuantity())
                 && request.orderNo().equals("order-contract-001")
                && request.skuId().equals("sku-contract-001")
                 )
                )).willReturn(sampleInventoryResponse);
       
        // Mocking the reserveInventory method to throw InsufficientInventoryException for specific test cases
        given(inventoryService.reserveInventory(
                 eq("reserve-order-insufficient-001-sku-contract-001")
                 ,
                    argThat(request -> Long.valueOf(100L).equals(request.reservedQuantity())
                    && request.orderNo().equals("order-insufficient-001")
                    && request.skuId().equals("sku-contract-001")
                    )
                )).willThrow(new InsufficientInventoryException("Inventory insufficient"));
       
                 

      
       // Mocking the behavior of the InventoryService for stock-in tests
    //    given(inventoryService.stockIn(anyString(), any(StockInRequest.class)))
    //             .willReturn(sampleInventoryResponse);

     
        // Create a sample InventoryResponse for release inventory tests
        InventoryResponse sampleReleaseInventoryResponse = new InventoryResponse(
                "sku-contract-001",
                100L,
                0L,
                100L,
                LocalDateTime.parse("2026-09-16T10:00:00"),
                LocalDateTime.parse("2026-09-16T10:00:00")
        );

       // Mocking the behavior of the InventoryService for release inventory tests
       given(inventoryService.releaseInventory(
        eq("release-order-contract-001-sku-contract-001"), 
        argThat(request -> request.orderNo().equals("order-contract-001")
                && request.skuId().equals("sku-contract-001")
                && Long.valueOf(10L).equals(request.releaseQuantity())
                )
        )).willReturn(sampleReleaseInventoryResponse);


         given(inventoryService.releaseInventory(
                 eq("release-order-not-found-sku-contract-001"),
                    argThat(request -> Long.valueOf(100L).equals(request.releaseQuantity())
                    && request.orderNo().equals("order-not-found-001")
                    && request.skuId().equals("sku-contract-001")
                    )
                )).willThrow(new ReservationNotFoundException("sku-contract-001", "order-not-found-001"));


    
        InventoryResponse sampleconfirmInventoryResponse = new InventoryResponse(
                "sku-contract-001",
                90L,
                0L,
                90L,
                LocalDateTime.parse("2026-09-16T10:00:00"),
                LocalDateTime.parse("2026-09-16T10:00:00")
        );
     
      // Mocking the behavior of the InventoryService for confirm reservation tests 
       given(inventoryService.confirmReservation(
        eq("confirm-order-contract-001-sku-contract-001"), 
        argThat(request -> request.orderNo().equals("order-contract-001")
                && request.skuId().equals("sku-contract-001")
                && Long.valueOf(10L).equals(request.confirmQuantity())
                )
        )).willReturn(sampleconfirmInventoryResponse);




        InventoryResponse sampleExpireInventoryResponse = new InventoryResponse(
                "sku-contract-001",
                100L,
                0L,
                100L,
                LocalDateTime.parse("2026-09-16T10:00:00"),
                LocalDateTime.parse("2026-09-16T10:00:00")
        );
                
         // Mocking the behavior of the InventoryService for expire reservation tests
       given(inventoryService.expireReservation(
        eq("expire-order-contract-001-sku-contract-001"), 
        argThat(request -> request.orderNo().equals("order-contract-001")
                && request.skuId().equals("sku-contract-001")
                && Long.valueOf(10L).equals(request.expireQuantity())
                )
        )).willReturn(sampleExpireInventoryResponse);

    }


    
}

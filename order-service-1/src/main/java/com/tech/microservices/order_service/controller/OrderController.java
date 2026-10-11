package com.tech.microservices.order_service.controller;

import com.tech.microservices.order_service.dto.*;
import com.tech.microservices.order_service.model.IdempotencyKeys;
import com.tech.microservices.order_service.model.Order;
import com.tech.microservices.order_service.model.OrderEventType;
import com.tech.microservices.order_service.model.OrderStatus;
import com.tech.microservices.order_service.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
//@RequestMapping("/api/order")
public class OrderController {
    private final OrderService orderService;

    // Create Order
    @PostMapping("/api/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<OrderResponse> createOrder(
            @RequestHeader("idempotencyKey") String idempotencyKey,
            @RequestBody @Valid OrderRequest orderRequest,
            UriComponentsBuilder uriBuilder)
    {
        OrderResponse response = orderService.createOrder(idempotencyKey,orderRequest);
        URI location = uriBuilder
               .path("/api/orders/{orderNumber}")
               .buildAndExpand(response.orderNumber())
               .toUri();
        return ResponseEntity.created(location).body(response);
    }

    // Update Order Status and return status is 200 OK
    @PatchMapping("/api/orders/{orderNumber}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @RequestHeader("idempotencyKey") String idempotencyKey,
            @PathVariable String orderNumber,
            @RequestBody @Valid UpdateOrderStatusRequest status)
    {
        OrderResponse response = orderService.updateOrderStatus(idempotencyKey,orderNumber, status.status(), OrderEventType.ORDER_STATUS_CHANGED);
        return ResponseEntity.ok(response);
    }


    // Confirm Paid Order Inventory
    @PatchMapping("/api/orders/{orderNumber}/inventory/confirm")
    public ResponseEntity<Void> confirmPaidOrderInventory(
            @RequestHeader("idempotencyKey") String idempotencyKey,
            @PathVariable String orderNumber)
    {
        orderService.confirmPaidOrderInventory(idempotencyKey,orderNumber);
        return ResponseEntity.noContent().build();
    }



    // Update Order Basic Info and return status is 200 OK
    @PatchMapping("/api/orders/{orderNumber}/basic")
    public ResponseEntity<OrderResponse> updateBasicOrderInfo(
            @RequestHeader("idempotencyKey") String idempotencyKey,
            @PathVariable String orderNumber,
            @RequestBody @Valid UpdateOrderBasicRequest request,
            UriComponentsBuilder uriBuilder)
    {
       OrderResponse response = orderService.updateOrderBasicInfo(idempotencyKey,orderNumber,request);
        return ResponseEntity.ok(response);
    }

    // Update Order Items and return status is 200 OK
    @PutMapping("/api/orders/{orderNumber}/items")
    public ResponseEntity<OrderResponse> updateOrderItem(
            @RequestHeader("idempotencyKey") String idempotencyKey,
            @PathVariable String orderNumber,
            @RequestBody @Valid UpdateOrderItemsRequest request
    )
    {
        OrderResponse response = orderService.updateOrderItems(idempotencyKey,orderNumber,request);
        return ResponseEntity.ok(response);
    }

    // Cancel Order and return status is 202 Accepted
    // 异步取消更适合返回 202 Accepted，表示请求已接受，但处理尚未完成
    @PatchMapping("/api/orders/{orderNumber}/cancel")
    public ResponseEntity<Void> cancelOrder(
            @RequestHeader("idempotencyKey") String idempotencyKey,
            @PathVariable String orderNumber) {
        orderService.cancelOrder(idempotencyKey,orderNumber);
        return ResponseEntity.accepted().build();
    }

    // Expire Order Inventory and return status is 202 Accepted
    // 异步过期更适合返回 202 Accepted，表示请求已接受
    @PatchMapping("/api/orders/{orderNumber}/inventory/expire")
    public ResponseEntity<Void> expireOrderInventory(
            @RequestHeader("idempotencyKey") String idempotencyKey,
            @PathVariable String orderNumber) {
        orderService.expireOrder(idempotencyKey,orderNumber);
        return ResponseEntity.accepted().build();
    }



    // Get All Orders
    @GetMapping("/api/orders")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<Page<OrderSummaryResponse>> getOrders(Pageable pageable) {
        return ResponseEntity.ok(orderService.getAllOrders(pageable)
                );
    }


    // Get Order By OrderNo
    @GetMapping("/api/orders/{orderNumber}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable String orderNumber) {
        return ResponseEntity.ok(orderService.getOrderById(orderNumber));
    }



    // Get Orders By UserID
    @GetMapping(value = "/api/orders", params = "userId")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<OrderResponse>> getOrdersByUserId(@RequestParam Long userId) {
        return ResponseEntity.ok(orderService.getOrdersByUserId(userId));
    }


}

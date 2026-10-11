package com.tech.microservices.inventory_service.controller;

import java.net.URI;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import com.tech.microservices.inventory_service.dto.InventoryConfirmRequest;
import com.tech.microservices.inventory_service.dto.InventoryReleaseRequest;
import com.tech.microservices.inventory_service.dto.InventoryRequest;
import com.tech.microservices.inventory_service.dto.InventoryReservationExpiredRequest;
import com.tech.microservices.inventory_service.dto.InventoryReservationRequest;
import com.tech.microservices.inventory_service.dto.InventoryReservationResponse;
import com.tech.microservices.inventory_service.dto.InventoryResponse;
import com.tech.microservices.inventory_service.dto.InventoryTransactionResponse;
import com.tech.microservices.inventory_service.dto.StockInRequest;
import com.tech.microservices.inventory_service.service.InventoryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;




    @GetMapping("/api/inventory/{skuId}")
    public ResponseEntity<InventoryResponse> getInventoryBySkuId(
        @PathVariable String skuId,
        UriComponentsBuilder uriBuilder) {
        InventoryResponse response = inventoryService.getInventoryBySkuId(skuId);
        return ResponseEntity.ok(response);
        
    }



    @GetMapping("/api/inventory/{skuId}/orderNo/{orderNo}")
    public ResponseEntity<InventoryResponse> getInventoryBySkuIdAndOrderNo(
        @PathVariable String skuId, 
        @PathVariable String orderNo) {
        InventoryResponse response = inventoryService.getInventoryBySkuIdAndOrderNo(skuId, orderNo);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/inventory")
    public ResponseEntity<Page<InventoryResponse>> getAllInventory(
        Pageable pageable) {
        Page<InventoryResponse> response = inventoryService.getAllInventory(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/inventory/{skuId}/reservations")
    public ResponseEntity<Page<InventoryReservationResponse>> getAllReservation(
        @PathVariable String skuId,
        Pageable pageable) {
        Page<InventoryReservationResponse> response = inventoryService.getAllReservation(skuId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/inventory/{skuId}/transactions")
    public ResponseEntity<Page<InventoryTransactionResponse>> getAllTransaction(
        @PathVariable String skuId,
        Pageable pageable) {
        Page<InventoryTransactionResponse> response = inventoryService.getAllTransaction(skuId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/inventory/orderNo/{orderNo}/reservations")
    public ResponseEntity<Page<InventoryReservationResponse>> getAllReservationByOrderNo(
        @PathVariable String orderNo,
        Pageable pageable) {
        Page<InventoryReservationResponse> response = inventoryService.getAllReservationByOrderNo(orderNo, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/inventory/orderNo/{orderNo}/transactions")
    public ResponseEntity<Page<InventoryTransactionResponse>> getAllTransactionByOrderNo(
        @PathVariable String orderNo,
        Pageable pageable) {
        Page<InventoryTransactionResponse> response = inventoryService.getAllTransactionByOrderNo(orderNo
, pageable);
        return ResponseEntity.ok(response);
    }


    @PostMapping("/api/inventory")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<InventoryResponse> createInventory(
        @RequestHeader("idempotencyKey") String idempotencyKey,
        @RequestBody @Valid InventoryRequest request,
        UriComponentsBuilder uriBuilder) 
        {
        InventoryResponse response = inventoryService.createInventory(idempotencyKey, request);
        URI location = uriBuilder
               .path("/api/inventory/{skuId}")
               .buildAndExpand(response.skuId())
               .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PostMapping("/api/inventory/stock-in")
    public ResponseEntity<InventoryResponse> stockInInventory(
        @RequestHeader("idempotencyKey") String idempotencyKey, 
        @RequestBody @Valid StockInRequest request,
        UriComponentsBuilder uriBuilder)
        {
        InventoryResponse response = inventoryService.stockIn(idempotencyKey, request);
        URI location = uriBuilder
               .path("/api/inventory/{skuId}")
               .buildAndExpand(response.skuId())
               .toUri();
        return ResponseEntity.created(location).body(response);
    }




    @PostMapping("/api/inventory/reserve")
    public ResponseEntity<InventoryResponse> reserveInventory(
        @RequestHeader("idempotencyKey") String idempotencyKey, 
        @RequestBody @Valid InventoryReservationRequest request) {
        InventoryResponse response = inventoryService.reserveInventory(idempotencyKey, request);
        URI location = UriComponentsBuilder
               .fromPath("/api/inventory/reserve/{skuId}")
               .buildAndExpand(response.skuId())
               .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PostMapping("/api/inventory/confirm")
    public ResponseEntity<InventoryResponse> confirmReservation(
        @RequestHeader("idempotencyKey") String idempotencyKey, 
        @RequestBody @Valid InventoryConfirmRequest request) {
            InventoryResponse response = inventoryService.confirmReservation(idempotencyKey, request);
            URI location = UriComponentsBuilder
                   .fromPath("/api/inventory/confirm/{skuId}")
                   .buildAndExpand(response.skuId())
                   .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PostMapping("/api/inventory/release")
    public ResponseEntity<InventoryResponse> releaseInventory(
        @RequestHeader("idempotencyKey") String idempotencyKey, 
        @RequestBody @Valid InventoryReleaseRequest request) {
        InventoryResponse response = inventoryService.releaseInventory(idempotencyKey, request);
        URI location = UriComponentsBuilder
               .fromPath("/api/inventory/{skuId}")
               .buildAndExpand(response.skuId())
               .toUri();
        return ResponseEntity.created(location).body(response);
    }




    @PostMapping("/api/inventory/return")
    public ResponseEntity<InventoryResponse> returnInventory(
        @RequestHeader("idempotencyKey") String idempotencyKey, 
        @RequestBody @Valid InventoryReservationExpiredRequest request) {
            InventoryResponse response = inventoryService.expireReservation(idempotencyKey, request);
            URI location = UriComponentsBuilder
                     .fromPath("/api/inventory/{skuId}")
                     .buildAndExpand(response.skuId())
                        .toUri();
        return ResponseEntity.created(location).body(response);
    }

    // @PostMapping("/api/inventroy/adjust")
    // public ResponseEntity<InventoryResponse> adjustInventory(
    //     @RequestHeader("idempotencyKey") String idempotencyKey, 
    //     @RequestBody InventoryAdjustmentRequest request) {
    //     InventoryResponse response = inventoryService.adjustInventory(idempotencyKey, request);
    //     URI location = UriComponentsBuilder
    //            .fromPath("/api/inventroy/{skuId}")
    //            .buildAndExpand(response.skuId())
    //            .toUri();
    //     return ResponseEntity.created(location).body(response);
    // }

    
}

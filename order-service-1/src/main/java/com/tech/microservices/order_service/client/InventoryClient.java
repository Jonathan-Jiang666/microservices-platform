package com.tech.microservices.order_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.tech.microservices.order_service.config.InventoryFeignConfig;
import com.tech.microservices.order_service.dto.InventoryConfirmRequest;
import com.tech.microservices.order_service.dto.InventoryReleaseRequest;
import com.tech.microservices.order_service.dto.InventoryReservationExpiredRequest;
import com.tech.microservices.order_service.dto.InventoryReservationRequest;
import com.tech.microservices.order_service.dto.InventoryResponse;


// Create a thread-safe inventory reservation service
// that prevents overselling under high concurrency

@FeignClient(
    value = "inventory-service", 
    url = "${inventory.service.url}", 
    configuration = InventoryFeignConfig.class)
public interface InventoryClient {

    @RequestMapping(method = RequestMethod.GET, value = "/api/inventory/{skuId}")
    InventoryResponse getInventoryBySkuId(
            @PathVariable("skuId") String skuId);


    @PostMapping("/api/inventory/reserve")
    InventoryResponse reserveInventory(
            @RequestHeader("idempotencyKey") String idempotencyKey,
            @RequestBody InventoryReservationRequest request) ;


    @PostMapping("/api/inventory/confirm")
    InventoryResponse confirmReservation(
            @RequestHeader("idempotencyKey") String idempotencyKey,
            @RequestBody InventoryConfirmRequest request);

    @PostMapping("/api/inventory/release")
    InventoryResponse releaseInventory(
            @RequestHeader("idempotencyKey") String idempotencyKey,
            @RequestBody InventoryReleaseRequest request) ;

    @PostMapping("/api/inventory/return")
    InventoryResponse returnInventory(
            @RequestHeader("idempotencyKey") String idempotencyKey,
            @RequestBody InventoryReservationExpiredRequest request) ;

}

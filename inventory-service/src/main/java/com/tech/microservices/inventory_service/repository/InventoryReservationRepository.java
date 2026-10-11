package com.tech.microservices.inventory_service.repository;

import com.tech.microservices.inventory_service.model.InventoryReservation;
import com.tech.microservices.inventory_service.model.InventoryTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {

    Page<InventoryReservation> findBySkuIdOrderByCreatedAtDesc(String skuId, Pageable pageable);

    Page<InventoryReservation> findByOrderNoOrderByCreatedAtDesc(String orderNo, Pageable pageable);

    Optional<InventoryReservation> findBySkuIdAndOrderNo(String skuId, String orderId);
}

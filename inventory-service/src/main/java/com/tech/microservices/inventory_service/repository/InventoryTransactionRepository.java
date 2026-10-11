package com.tech.microservices.inventory_service.repository;

import com.tech.microservices.inventory_service.model.InventoryTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {

    Page<InventoryTransaction> findBySkuIdOrderByCreatedAtDesc(String skuId, Pageable pageable);

    Page<InventoryTransaction> findByOrderNoOrderByCreatedAtDesc(String orderNo, Pageable pageable);

    Page<InventoryTransaction> findBySkuIdAndOrderNo(String skuId, String orderNo, Pageable pageable);
}


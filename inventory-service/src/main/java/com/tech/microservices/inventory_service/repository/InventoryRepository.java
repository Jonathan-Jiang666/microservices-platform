package com.tech.microservices.inventory_service.repository;

import com.tech.microservices.inventory_service.model.Inventory;
import com.tech.microservices.inventory_service.model.InventoryReservation;
import com.tech.microservices.inventory_service.model.InventoryTransaction;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long>, JpaSpecificationExecutor<Inventory> {
    Page<Inventory> findAllBy(Pageable pageable);

    boolean existsBySkuId(String skuId);

    Inventory findBySkuId(String skuId);

    // Pessimistic lock
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.skuId = :skuId")
    Optional<Inventory> findBySkuIdForUpdate(@Param("skuId") String skuId);
}



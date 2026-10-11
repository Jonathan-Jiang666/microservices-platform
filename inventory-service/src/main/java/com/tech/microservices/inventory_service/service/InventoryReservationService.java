package com.tech.microservices.inventory_service.service;

import com.tech.microservices.inventory_service.dto.InventoryReservationResponse;
import com.tech.microservices.inventory_service.exception.ReservationNotFoundException;
import com.tech.microservices.inventory_service.model.InventoryReservation;
import com.tech.microservices.inventory_service.repository.InventoryReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class InventoryReservationService {

    private final InventoryReservationRepository inventoryReservationRepository;

    public InventoryReservation saveNewInventoryReservation(InventoryReservation inventoryReservation)
    {
        return this.inventoryReservationRepository.save(inventoryReservation);
    }

    public Page<InventoryReservationResponse> getAllReservation(String skuId, Pageable pageable)
    {
        Page<InventoryReservation> inventoryReservationList = this.inventoryReservationRepository.findBySkuIdOrderByCreatedAtDesc(skuId,pageable);
        return inventoryReservationList.map( this::toInventoryReservationResponse );
    }

    public Page<InventoryReservationResponse> getAllReservationByOrderNo(String orderNo, Pageable pageable)
    {
        Page<InventoryReservation> inventoryReservationList = this.inventoryReservationRepository.findByOrderNoOrderByCreatedAtDesc(orderNo,pageable);
        return inventoryReservationList.map( this::toInventoryReservationResponse );
    }

    public InventoryReservation findBySkuIdAndOrderNo(String skuId, String orderNo)
    {
        return this.inventoryReservationRepository.
               findBySkuIdAndOrderNo(skuId, orderNo).orElseThrow(() ->new ReservationNotFoundException(skuId, orderNo));
    }

    public Page<InventoryReservationResponse> getAllReservation(Pageable pageable)
    {
        Page<InventoryReservation> inventoryReservationList = this.inventoryReservationRepository.findAll(pageable);
        return inventoryReservationList.map( this::toInventoryReservationResponse );
    }

    
    private InventoryReservationResponse toInventoryReservationResponse(InventoryReservation inventoryReservation)
    {
        return new InventoryReservationResponse(
                inventoryReservation.getReservationNo(),
                inventoryReservation.getOrderNo(),
                inventoryReservation.getSkuId(),
                inventoryReservation.getQuantity(),
                inventoryReservation.getStatus(),
                inventoryReservation.getExpiresAt(),
                inventoryReservation.getCreatedAt(),
                inventoryReservation.getUpdatedAt()
        );
    }
}

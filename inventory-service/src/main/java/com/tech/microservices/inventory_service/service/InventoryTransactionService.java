package com.tech.microservices.inventory_service.service;

import com.tech.microservices.inventory_service.dto.InventoryTransactionResponse;
import com.tech.microservices.inventory_service.exception.ReservationNotFoundException;
import com.tech.microservices.inventory_service.model.InventoryReservation;
import com.tech.microservices.inventory_service.model.InventoryTransaction;
import com.tech.microservices.inventory_service.repository.InventoryTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class InventoryTransactionService  {

    private final InventoryTransactionRepository inventoryTransactionRepository;

    public InventoryTransaction saveNewInventoryTransaction(InventoryTransaction inventoryTransaction)
    {
       return this.inventoryTransactionRepository.save(inventoryTransaction);
    }

    public Page<InventoryTransactionResponse> getAllTransaction(String skuId, Pageable pageable)
    {
        Page<InventoryTransaction> inventoryTransactionList = this.inventoryTransactionRepository.findBySkuIdOrderByCreatedAtDesc(skuId,pageable);
        return inventoryTransactionList.map( this::toInventoryTransactionResponse );
    } 

    public Page<InventoryTransactionResponse> getAllTransactionByOrderNo(String orderNo, Pageable pageable)
    {
        Page<InventoryTransaction> inventoryTransactionList = this.inventoryTransactionRepository.findByOrderNoOrderByCreatedAtDesc(orderNo,pageable);
        return inventoryTransactionList.map( this::toInventoryTransactionResponse );
    }

    public Page<InventoryTransactionResponse> getBySkuIdAndOrderNo(String skuId, String orderNo,Pageable pageable) 
    {
         Page<InventoryTransaction> inventoryTransactionList = this.inventoryTransactionRepository.findBySkuIdAndOrderNo(skuId, orderNo, pageable);
            return inventoryTransactionList.map( this::toInventoryTransactionResponse );
   }

   public Page<InventoryTransactionResponse> getAllTransaction(Pageable pageable)
   {
        Page<InventoryTransaction> inventoryTransactionList = this.inventoryTransactionRepository.findAll(pageable);
        return inventoryTransactionList.map( this::toInventoryTransactionResponse );
   }
    private InventoryTransactionResponse toInventoryTransactionResponse(InventoryTransaction inventoryTransaction)
    {
        return new InventoryTransactionResponse(
                inventoryTransaction.getTransactionNo(),
                inventoryTransaction.getReservedId(),
                inventoryTransaction.getSkuId(),
                inventoryTransaction.getOrderNo(),
                inventoryTransaction.getReservationNo(),
                inventoryTransaction.getTransactionType(),
                inventoryTransaction.getQuantity(),
                inventoryTransaction.getQuantityBefore(),
                inventoryTransaction.getQuantityAfter(),
                inventoryTransaction.getReferenceId(),
                inventoryTransaction.getCreatedAt()
        );
    }

}

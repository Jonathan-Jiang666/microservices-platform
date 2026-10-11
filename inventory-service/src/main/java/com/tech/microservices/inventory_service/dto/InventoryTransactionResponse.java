package com.tech.microservices.inventory_service.dto;

import com.tech.microservices.inventory_service.model.Inventory;
import com.tech.microservices.inventory_service.model.TransactionType;
import jakarta.persistence.*;

import java.time.LocalDateTime;

public record InventoryTransactionResponse(

 String transactionNo,

 Long reservedId,

 String skuId,

 String orderNo,

 String reservationNo,

 TransactionType transactionType,

 Long quantity,

 Long quantityBefore,

 Long quantityAfter,

 String referenceId,

 LocalDateTime createdAt
) {
}

package com.tech.microservices.inventory_service.dto;

import java.time.LocalDateTime;
import java.util.List;

public record InventoryResponse(

        String skuId,
        Long onHandQuantity,
        Long reservedQuantity,
        Long availableQuantity,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
        //List<InventoryReservationResponse> reservations,
        //List<InventoryTransactionResponse> transactions
) {
}

package com.tech.microservices.inventory_service.dto.event.item;

import java.time.LocalDateTime;

public record InventoryReservationConfirmedEvent(

        Long inventoryId,
        String skuId,
        String orderNo,
        String reservationNo,
        String transactionNo,
        Long confirmQuantity,
        Long onHandBefore,
        Long onHandAfter,
        Long reservedBefore,
        Long reservedAfter,
        Long availableBefore,
        Long availableAfter,
        LocalDateTime createAt
) {
}

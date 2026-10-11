package com.tech.microservices.inventory_service.dto;

import com.tech.microservices.inventory_service.model.ReservationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

public record InventoryReservationResponse(

 String reservationNo,
 String orderNo,
 String skuId,
 Long quantity,
 ReservationStatus status,
 LocalDateTime expiresAt,
 LocalDateTime createdAt,
 LocalDateTime updatedAt
) {
}

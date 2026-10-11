package com.tech.microservices.inventory_service.exception;

public class DuplicateReservationException extends RuntimeException{
    public DuplicateReservationException(String orderNo, String skuId) {
        super("Reservation already exists for orderNo:" + orderNo + " and skuId:" + skuId);
    }
}

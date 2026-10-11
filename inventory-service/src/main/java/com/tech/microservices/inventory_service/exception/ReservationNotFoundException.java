package com.tech.microservices.inventory_service.exception;

public class ReservationNotFoundException extends RuntimeException{
    public ReservationNotFoundException(String skuId, String orderNo) {
        super("Reservation not found :" +"skuId = " + skuId +" , orderNo = " + orderNo);
    }
}

package com.tech.microservices.inventory_service.exception;

import lombok.Getter;

@Getter 
public class InventoryReservationCommandFailureException extends RuntimeException {
    
    private final String orderNumber;
    private final String failedSkuId;
    private final String reasonCode;

    public InventoryReservationCommandFailureException(
        String orderNumber,
        String failedSkuId,
        String reasonCode,
        String message,
        Throwable cause
    ) {
        super(message, cause);
        this.orderNumber = orderNumber;
        this.failedSkuId = failedSkuId;
        this.reasonCode = reasonCode;
    }

}

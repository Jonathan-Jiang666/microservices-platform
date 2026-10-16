package com.tech.microservices.order_service.service;

import org.springframework.stereotype.Service;


import com.tech.microservices.order_service.integration.inventory.event.InventoryConfirmationFailedEvent;
import com.tech.microservices.order_service.integration.inventory.event.InventoryExpirationFailedEvent;
import com.tech.microservices.order_service.integration.inventory.event.InventoryReleaseCompletedEvent;
import com.tech.microservices.order_service.integration.inventory.event.InventoryReleaseFailedEvent;
import com.tech.microservices.order_service.integration.inventory.event.InventoryReservationFailedEvent;
import com.tech.microservices.order_service.model.OrderEventType;
import com.tech.microservices.order_service.model.OrderStatus;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;


@Service 
@RequiredArgsConstructor
public class OrderCommandService {
    
    
    private final OrderService orderService;


    @Transactional
    public void markOrderInventoryReservedStatus(
        String eventID,
        String orderNumber
        ) 
    {
        String idempotencyKey = "inventory-result-"+eventID;
        this.orderService.updateOrderStatus(
        idempotencyKey, 
        orderNumber, 
        OrderStatus.AWAITING_PAYMENT,
        OrderEventType.ORDER_INVENTORY_RESERVED
        );
        
    }

    @Transactional
    public void markOrderInventoryFailedStatus(
        String eventID, 
        InventoryReservationFailedEvent event
    )
    {
        String idempotencyKey = "inventory-result-"+eventID;
        this.orderService.updateOrderStatus(
        idempotencyKey, 
        event.orderNumber(), 
        OrderStatus.FAILED,
        OrderEventType.ORDER_INVENTORY_FAILED
        );
    }

    @Transactional
    public void markOrderReleaseCompletedStatus(
        String eventID, 
        InventoryReleaseCompletedEvent command
    )
    {
        String idempotencyKey = "inventory-result-"+eventID;

        switch(command.reason())
        {
            case ORDER_CANCELLATION -> {
                this.orderService.updateOrderStatus(
                    idempotencyKey, 
                    command.orderNumber(), 
                    OrderStatus.CANCELLED,
                    OrderEventType.ORDER_CANCELLED
                );
            }
            case ORDER_EXPIRATION -> {
                this.orderService.updateOrderStatus(
                    idempotencyKey, 
                    command.orderNumber(), 
                    OrderStatus.EXPIRED,
                    OrderEventType.ORDER_EXPIRED
                );
            }
            default -> throw new IllegalArgumentException("Unexpected value: " + command.reason());
        }
    }

    @Transactional 
    public void markOrderInventoryReleaseFailedStatus
    ( 
        String eventID, 
        InventoryReleaseFailedEvent command)
    {
        String idempotencyKey = "inventory-result-"+eventID;
        this.orderService.updateOrderStatus(
        idempotencyKey, 
        command.orderNumber(), 
        OrderStatus.CANCELLATION_FAILED,
        OrderEventType.ORDER_CANCELLATION_FAILED
        );
    }

    @Transactional
    public void markOrderExpiredStatus(
        String eventID, 
        String orderNumber
    )
    {
        String idempotencyKey = "inventory-result-"+eventID;
        this.orderService.updateOrderStatus(
        idempotencyKey, 
        orderNumber, 
        OrderStatus.EXPIRED,
        OrderEventType.ORDER_EXPIRED
        );
    }

    @Transactional 
    public void markOrderExpiredFailedStatus(
        String eventID, 
        InventoryExpirationFailedEvent command
    )
    {
        String idempotencyKey = "inventory-result-"+eventID;
        this.orderService.updateOrderStatus(
        idempotencyKey, 
        command.orderNumber(), 
        OrderStatus.FAILED,
        OrderEventType.ORDER_EXPIRATION_FAILED
        );
    }


    @Transactional 
    public void markOrderConfirmedStatus(
        String eventID, 
        String orderNumber
    )
    {
        String idempotencyKey = "inventory-result-"+eventID;
        this.orderService.updateOrderStatus(
        idempotencyKey, 
        orderNumber, 
        OrderStatus.COMPLETED,
        OrderEventType.ORDER_CONFIRMED
        );
    }

    @Transactional 
    public void markOrderConfirmationFailedStatus(
        String eventID, 
        InventoryConfirmationFailedEvent command
    )
    {
        String idempotencyKey = "inventory-result-"+eventID;
        this.orderService.updateOrderStatus(
        idempotencyKey, 
        command.orderNumber(), 
        OrderStatus.FAILED,
        OrderEventType.ORDER_CONFIRMATION_FAILED
        );
    }

    @Transactional 
    public void markOrderRefundedStatus(
        String eventID, 
        String orderNumber
    )
    {
        String idempotencyKey = "inventory-result-"+eventID;
        this.orderService.updateOrderStatus(
        idempotencyKey, 
        orderNumber, 
        OrderStatus.REFUNDED,
        OrderEventType.ORDER_REFUNDED
        );
    }
}
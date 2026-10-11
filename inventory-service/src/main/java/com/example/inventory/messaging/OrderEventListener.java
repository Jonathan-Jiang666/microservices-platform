package com.example.inventory.messaging;

import com.example.inventory.domain.InventoryService;
import com.example.inventory.domain.OrderCreatedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListener {

    private final InventoryService inventoryService;

    public OrderEventListener(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @KafkaListener(topics = "order-events", groupId = "inventory-service")
    public void onOrderCreated(OrderCreatedEvent event, @Header(name = "messageId", required = false) String messageId) {
        String idempotencyKey = messageId != null ? messageId : String.valueOf(event.orderId());
        inventoryService.processOrderCreated(idempotencyKey, event);
    }
}

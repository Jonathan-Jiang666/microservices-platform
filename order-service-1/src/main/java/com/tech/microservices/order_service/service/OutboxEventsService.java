package com.tech.microservices.order_service.service;

import com.tech.microservices.order_service.integration.inventory.command.InventoryCommandType;
import com.tech.microservices.order_service.model.Order;
import com.tech.microservices.order_service.model.OrderEventType;
import com.tech.microservices.order_service.model.OrderStatus;
import com.tech.microservices.order_service.model.OutboxEventStatus;
import com.tech.microservices.order_service.model.OutboxEvents;
import com.tech.microservices.order_service.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxEventsService {

    private final OutboxEventRepository outboxEventRepository;

    @Value("${order.outbox.topic}")
    private String orderEventsTopic;

    @Value("${inventory.command.topic}")
    private String inventoryCommandsTopic;

   // Write outbox 
   // 写入 outbox
    public void saveOutboxEvent(OutboxEvents outboxEvents) {
        outboxEventRepository.save(outboxEvents);
        log.info("Outbox event saved successfully, the id is {}", outboxEvents.getId());
    }

    // Read outbox events pending
    List<OutboxEvents> findPendingEvents() {
        return outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING.name());
    }

    // Mark outbox as failed
    public void markFailed(OutboxEvents events)
    {
        events.setStatus(OutboxEventStatus.FAILED.name());
        events.setPublishedAt(LocalDateTime.now());
        outboxEventRepository.save(events);
        log.info("Outbox event marked as failed, the id is {}", events.getId());
    }

    public void markPublished(OutboxEvents events)
    {
        events.setStatus(OutboxEventStatus.PUBLISHED.name());
        events.setPublishedAt(LocalDateTime.now());
        outboxEventRepository.save(events);
        log.info("Outbox event marked as published,the id is {}",events.getId());
    }

    // Order items changed event
    public void createOrderItemsChangedEvent(Order order, String payload) {
        this.createOrderEvent(order, OrderEventType.ORDER_ITEMS_CHANGED.name(), orderEventsTopic, payload);
    }

    // Order basic info changed event
   public void createOrderBasicInfoChangedEvent(Order order, String payload) {
        this.createOrderEvent(order, OrderEventType.ORDER_BASIC_INFO_CHANGED.name(), orderEventsTopic, payload);
    }

    // Order created event
    public void createOrderCreatedEvent(Order order, String payload) {
      this.createOrderEvent(order, OrderEventType.ORDER_CREATED.name(), orderEventsTopic, payload);
    }


    // Order cancelled event
    public void createOrderCancelledEvent(Order order, String payload) {
        this.createOrderEvent(order, OrderEventType.ORDER_CANCELLED.name(), orderEventsTopic,payload);
    }

    // Order confirmed event
    public void createOrderConfirmedEvent(Order order, String payload) {
        this.createOrderEvent(order, OrderEventType.ORDER_CONFIRMED.name(), orderEventsTopic,payload);
    }

    public void createOrderExpiredEvent(Order order, String payload) {
        this.createOrderEvent(order, OrderEventType.ORDER_EXPIRED.name(), orderEventsTopic,payload);
    }

    // Order status changed event
    public void createOrderStatusChangedEvent(Order order, String payload) {
        this.createOrderEvent(order, OrderEventType.ORDER_STATUS_CHANGED.name(), orderEventsTopic,payload);
    }


    // Inventory reservation requested event 
    // 用于通知下游订单预定事件和状态
    public void createInventoryReservationRequestedEvent(Order order, String payload) {
        this.createOrderEvent(order, InventoryCommandType.INVENTORY_RESERVATION_REQUESTED.name(), inventoryCommandsTopic, payload);
    }

    // Inventory release requested event
    // 用于通知下游订单释放事件和状态
    public void createInventoryReleaseRequestedEvent(Order order, String payload) {
        this.createOrderEvent(order, InventoryCommandType.INVENTORY_RELEASE_REQUESTED.name(), inventoryCommandsTopic, payload);
    }

    // Order inventory reserved failed event
    // 用于通知下游订单库存预定失败事件和状态
    public void createOrderInventoryFailedEvent(Order order, String payload) {
        this.createOrderEvent(order, OrderEventType.ORDER_INVENTORY_FAILED.name(), orderEventsTopic, payload);
    }

    public void createOrderStatusEvent(
        Order order, 
        OrderEventType eventType,
        String payload) {
        this.createOrderEvent(order, eventType.name(), orderEventsTopic, payload);
    }


    // Create the unique and common method to create outbox event by order, event type and payload
    // 创建唯一的通用写入方法
    private void createOrderEvent(Order order, 
    String eventType, 
    String destinationTopic,
    String payload) {
        this.saveOutboxEvent(
                OutboxEvents.builder()
                        .aggregateId(order.getOrderNumber())
                        .aggregateType("ORDER")
                        .eventType(eventType)
                        .destinationTopic(destinationTopic)
                        .payload(payload)
                        .status(OutboxEventStatus.PENDING.name())
                        .retryCount(0)
                        .build()
        );
    }
}

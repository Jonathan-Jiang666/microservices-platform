package com.tech.microservices.inventory_service.service;


import com.tech.microservices.inventory_service.model.Inventory;
import com.tech.microservices.inventory_service.model.InventoryItemEventType;
import com.tech.microservices.inventory_service.model.InventoryOrderEventType;
import com.tech.microservices.inventory_service.model.OutboxEventStatus;
import com.tech.microservices.inventory_service.model.OutboxEvents;
import com.tech.microservices.inventory_service.repository.OutboxEventRepository;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxEventService {

    private final OutboxEventRepository outboxEventRepository;

    // Write outbox event
    public void saveOutboxEvent(OutboxEvents outboxEvents)
    {
        this.outboxEventRepository.save(outboxEvents);
        log.info("OutboxEvent saved successfully, this id is {}", outboxEvents.getId());
    }

    // Mark outbox event as failed
    public void markFailed(OutboxEvents outboxEvents)
    {
        outboxEvents.setStatus(OutboxEventStatus.FAILED.name());
        outboxEvents.setPublishedAt(LocalDateTime.now());
        outboxEventRepository.save(outboxEvents);
        log.info("OutboxEvent marked as failed, this id is {}", outboxEvents.getId());
    }

    public void markPublished(OutboxEvents outboxEvents)
    {
        outboxEvents.setStatus(OutboxEventStatus.PUBLISHED.name());
        outboxEvents.setPublishedAt(LocalDateTime.now());
        outboxEventRepository.save(outboxEvents);
        log.info("OutboxEvent marked as published, this id is {}", outboxEvents.getId());
    }

    public void createInventoryReservationExpiredEvent(Inventory inventory, String payload)
    {
        this.createInventoryEvent(inventory, InventoryItemEventType.INVENTORY_RESERVATION_EXPIRED.name(), payload);
    }



    public void createInventoryConfirmedEvent(Inventory inventory, String payload)
    {
        this.createInventoryEvent(inventory, InventoryItemEventType.INVENTORY_RESERVATION_CONFIRMED.name(), payload);
    }


    // Create event for inventory release
    public void createInventoryReleaseEvent(Inventory inventory, String payload)
    {
        this.createInventoryEvent(inventory, InventoryItemEventType.INVENTORY_RESERVATION_RELEASED.name(), payload);
    }


    // Create event for stock in
    public void createStockInEvent(Inventory inventory, String payload)
    {
        this.createInventoryEvent(inventory, InventoryItemEventType.INVENTORY_STOCKED_IN.name(), payload);
    }

    // Create event for inventory created
    public void createInventoryCreatedEvent(Inventory inventory, String payload)
    {
        this.createInventoryEvent(inventory, InventoryItemEventType.INVENTORY_CREATED.name(), payload);
    }

    // Create event for inventory reserved
    public void createReservededInventoryEvent(Inventory inventory, String payload)
    {
        this.createInventoryEvent(inventory, InventoryItemEventType.INVENTORY_RESERVED.name(), payload);
    }

    
    // Create event by event type, payload and save to outbox
    private void createInventoryEvent(Inventory inventory, String eventType, String payload) {
        this.saveOutboxEvent(
                OutboxEvents.builder()
                        .aggregateId(inventory.getSkuId())
                        .aggregateType("INVENTORY")
                        .eventType(eventType)
                        .payload(payload)
                        .status(OutboxEventStatus.PENDING.name())
                        .retryCount(0)
                        .build()
        );
    }

    // 增加失败outbox事件的创建方法，用于库存预定失败的情况
    public void createInventoryReservationFailedEvent(String orderNumber, String payload)
    {
        OutboxEvents outboxEvent = OutboxEvents.builder()
            .aggregateId(orderNumber)
            .aggregateType("ORDER")
            .eventType(
                    InventoryOrderEventType
                            .INVENTORY_RESERVATION_FAILED
                            .name()
            )
            .payload(payload)
            .status(OutboxEventStatus.PENDING.name())
            .retryCount(0)
            .build();
            this.saveOutboxEvent(outboxEvent);
    }

    //整个订单的库存预定完成事件 entire order inventory reservation completed event
    public void createInventoryReservationCompletedEvent(String orderNumber, String payload)
    {
        OutboxEvents outboxEvent = OutboxEvents.builder()
            .aggregateId(orderNumber)
            .aggregateType("ORDER")
            .eventType(
                    InventoryOrderEventType
                            .INVENTORY_RESERVATION_COMPLETED
                            .name()
            )
            .payload(payload)
            .status(OutboxEventStatus.PENDING.name())
            .retryCount(0)
            .build();

    this.saveOutboxEvent(outboxEvent);
    }


    //整个订单的释放完成事件 entire order inventory release completed event
    public void createInventoryReleaseCompletedEvent(String orderNumber, String payload)
    {
        OutboxEvents outboxEvent = OutboxEvents.builder()
            .aggregateId(orderNumber)
            .aggregateType("ORDER")
            .eventType(
                    InventoryOrderEventType
                            .INVENTORY_RELEASE_COMPLETED
                            .name()
            )
            .payload(payload)
            .status(OutboxEventStatus.PENDING.name())
            .retryCount(0)
            .build();

       this.saveOutboxEvent(outboxEvent);
    }

    // 创建释放失败事件的记录 Create release failed event record
    public void createInventoryReleaseFailedEvent(String orderNumber, String payload)
    {
        OutboxEvents outboxEvent = OutboxEvents.builder()
            .aggregateId(orderNumber)
            .aggregateType("ORDER")
            .eventType(
                    InventoryOrderEventType
                            .INVENTORY_RELEASE_FAILED
                            .name()
            )
            .payload(payload)
            .status(OutboxEventStatus.PENDING.name())
            .retryCount(0)
            .build();
            this.saveOutboxEvent(outboxEvent);
    }


}

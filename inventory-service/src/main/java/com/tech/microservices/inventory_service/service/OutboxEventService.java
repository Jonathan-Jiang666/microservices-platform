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
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxEventService {

    private final OutboxEventRepository outboxEventRepository;

    @Value("${inventory.outbox.max-attempts:4}") // 默认最大尝试次数为 4 次，包括首次发送
    private int maxAttempts = 4; 

    // Write outbox event
    public void saveOutboxEvent(OutboxEvents outboxEvents)
    {
        if (outboxEvents.getNextAttemptAt() == null) {
           outboxEvents.setNextAttemptAt(LocalDateTime.now());
        }

         this.outboxEventRepository.save(outboxEvents);
         log.info(
             "OutboxEvent saved successfully, this id is {}",
             outboxEvents.getId()
           );  
    }


    // Read outbox events pending
    public List<OutboxEvents> findPendingEvents() {
        // 只读取已经到达 nextAttemptAt 的消息，避免失败消息每 3 秒立即重试。
        return outboxEventRepository
                .findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                        OutboxEventStatus.PENDING.name(),
                        LocalDateTime.now()
                );
    }

    // Mark outbox event as failed
    // 原来是失败直接标记失败 会影响到后续的重试机制，改为增加重试次数，超过最大重试次数才标记为失败
    public void markFailed(OutboxEvents outboxEvents, Throwable error)
    {
        // outboxEvents.setStatus(OutboxEventStatus.FAILED.name());
        // outboxEvents.setPublishedAt(LocalDateTime.now());
        // outboxEventRepository.save(outboxEvents);
        // log.info("OutboxEvent marked as failed, this id is {}", outboxEvents.getId());

        int failedCount = outboxEvents.getRetryCount() + 1;
        boolean exhausted = failedCount >= maxAttempts;

        outboxEvents.setRetryCount(failedCount);
        outboxEvents.setPublishedAt(null);
        outboxEvents.setLastError(error.getMessage());


        if (exhausted)
        {
            outboxEvents.setStatus(OutboxEventStatus.FAILED.name());
            outboxEvents.setFailedAt(LocalDateTime.now());
            outboxEvents.setNextAttemptAt(null);
        } else {
            outboxEvents.setStatus(OutboxEventStatus.PENDING.name());
            outboxEvents.setFailedAt(null);
            outboxEvents.setNextAttemptAt(
                    calculateNextAttemptAt(failedCount)
            );
        }
        outboxEventRepository.save(outboxEvents);

    }

    /**
     * 计算下一次允许发布的时间。
     *
     * <p>这里实现的是 randomized exponential backoff with jitter ---带随机抖动的指数退避
     * 带随机抖动的指数退避。失败次数越多，下一次重试等待越久；
     * 同时增加少量随机秒数，避免大量消息在同一时刻一起重试。
     *
     * <p>注意：这个方法只负责计算并返回时间。
     * Publisher 查询 PENDING 消息时，必须使用 next_attempt_at <= 当前时间作为条件，
     * 否则虽然数据库保存了下次时间，消息仍然可能每 3 秒被重复取出。
     */
    private LocalDateTime calculateNextAttemptAt(int attemptCount) {
        // 第一次失败后至少等待 5 秒。
        long initialDelaySeconds = 5;

        // 退避时间最多 300 秒，避免等待时间无限增长。
        long maxDelaySeconds = 300;

        // attemptCount 从 1 开始：
        // 第 1 次失败 -> exponent = 0 -> 2^0 = 1
        // 第 2 次失败 -> exponent = 1 -> 2^1 = 2
        // 第 3 次失败 -> exponent = 2 -> 2^2 = 4
        // 最大限制为 6，防止位移结果继续快速扩大。
        int exponent = Math.min(attemptCount - 1, 6);

        // 1L << exponent 是对数字 1 做二进制左移，效果等同于 2^exponent。
        // 因此退避时间是：5、10、20、40、80、160、320 秒。
        long exponentialDelaySeconds =
                initialDelaySeconds * (1L << exponent);

        // 将指数退避时间限制在 maxDelaySeconds 以内。
        // 例如 320 秒会被限制为 300 秒。
        long delaySeconds = Math.min(
                exponentialDelaySeconds,
                maxDelaySeconds
        );

        // jitter 的专业含义是“抖动”或“随机扰动”。
        // nextLong(0, 3) 返回 0、1 或 2，右边界 3 不包含在结果内。
        // 例如相同失败次数的消息，不会全部精确地在 10 秒后同时重试。
        long jitterSeconds =
                ThreadLocalRandom.current().nextLong(0, 3);

        // LocalDateTime.now() 是当前时间。
        // 最终下次重试时间 = 当前时间 + 指数退避时间 + 随机抖动时间。
        return LocalDateTime.now()
                .plusSeconds(delaySeconds + jitterSeconds);
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
            .nextAttemptAt(LocalDateTime.now())
            .build();
            this.saveOutboxEvent(outboxEvent);
    }


}

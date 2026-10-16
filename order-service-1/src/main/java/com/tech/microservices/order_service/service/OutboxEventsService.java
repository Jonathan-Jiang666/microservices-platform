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
import java.util.concurrent.ThreadLocalRandom;

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

    @Value ("${order.outbox.max-attempts:4}") // 默认最大尝试次数为 4 次，包括首次发送
    private int maxAttempts = 4;

   // Write outbox 
   // 写入 outbox
    public void saveOutboxEvent(OutboxEvents outboxEvents) {
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
    List<OutboxEvents> findPendingEvents() {
        // 只读取已经到达 nextAttemptAt 的消息，避免失败消息每 3 秒立即重试。
        return outboxEventRepository
            .findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                OutboxEventStatus.PENDING.name(),
                LocalDateTime.now()
            );
    }

    // Mark outbox as failed
    // 保持和Order侧一样的处理机制，原来是失败直接标记失败 会影响到后续的重试机制，改为增加重试次数，超过最大重试次数才标记为失败
    public void markFailed(OutboxEvents events, Throwable error)
    {
        // events.setStatus(OutboxEventStatus.FAILED.name());
        // events.setPublishedAt(LocalDateTime.now());
        // outboxEventRepository.save(events);
        // log.info("Outbox event marked as failed, the id is {}", events.getId());

        int failedCount = events.getRetryCount() + 1;
        boolean exhausted = failedCount >= maxAttempts;

        events.setRetryCount(failedCount);
        events.setPublishedAt(null);
        events.setLastError(error.getMessage());


        if (exhausted) 
        {
        events.setStatus(OutboxEventStatus.FAILED.name());
        events.setFailedAt(LocalDateTime.now());
        events.setNextAttemptAt(null);
        } else {
        events.setStatus(OutboxEventStatus.PENDING.name());
        events.setFailedAt(null);
        events.setNextAttemptAt(
            calculateNextAttemptAt(failedCount)
        );
        }
        outboxEventRepository.save(events);
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
                        // 新消息允许立即进行第一次发布；退避只从失败后开始。
                        .nextAttemptAt(LocalDateTime.now())
                        .build()
        );
    }
}

package com.tech.microservices.order_service.integration.inventory.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.tech.microservices.order_service.service.OrderCommandService;
import com.tech.microservices.order_service.integration.inventory.event.InventoryConfirmationFailedEvent;
import com.tech.microservices.order_service.integration.inventory.event.InventoryExpirationFailedEvent;
import com.tech.microservices.order_service.integration.inventory.event.InventoryReleaseCompletedEvent;
import com.tech.microservices.order_service.integration.inventory.event.InventoryReleaseFailedEvent;
import com.tech.microservices.order_service.integration.inventory.event.InventoryReservationFailedEvent;
import com.tech.microservices.order_service.integration.inventory.event.InventoryResultEventType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;


@Component 
@RequiredArgsConstructor
@Slf4j 
public class InventoryResultConsumer {
    


// 1. 事件信封示例
// {
// 	"data": {
// 		"items": [
// 			{
// 				"skuId": "sku-postman-outbox-001",
// 				"quantity": 1
// 			}
// 		],
// 		"completedAt": "2026-09-22T14:56:31.465813",
// 		"orderNumber": "order-publisher-006",
// 		"commandEventId": "129d4b8c-99e5-4254-82e3-beb5d356c08e"
// 	},
// 	"eventId": "32ea68e6-0d60-45b4-b12c-e5f23c86d084",
// 	"eventType": "INVENTORY_RESERVATION_COMPLETED",
// 	"occurredAt": "2026-09-22T14:56:31.466162",
// 	"aggregateId": "order-publisher-006",
// 	"eventVersion": 1,
// 	"aggregateType": "ORDER"
//   }
    
    private final ObjectMapper objectMapper;
    private final OrderCommandService orderCommandService;

    @KafkaListener (
        topics = "${inventory.event.topic}", 
        groupId = "${spring.kafka.consumer.group-id}"
    )

    public void consumeInventoryResult( String message)
    {
        try {
            // 1. 解析最外层事件信封
            JsonNode envelope = objectMapper.readTree(message);

            // 2. 获取 Inventory Service 发布的结果事件类型。
            // eventType 由 Inventory Service 的 InventoryOrderEventType 枚举生成，
            // 生产端位置：inventory-service/model/InventoryOrderEventType.java。
            String eventType = envelope
                    .get("eventType")
                    .asText();

            // 3. 当前 Consumer 只处理库eventId与订单号相同的 且 只处理状态是完整的
            // if (!"INVENTORY_RESERVATION_COMPLETED".equals(eventType) && !"INVENTORY_RESERVATION_FAILED".equals(eventType)) {
            //     log.debug(
            //             "Ignoring event with mismatched order number: {}",
            //             eventType
            //     );
            //     return;
            // }
          
            InventoryResultEventType resultEventType = InventoryResultEventType.valueOf(eventType);

  
            String eventId = envelope
             .get("eventId")
             .asText();   
            
            String orderNumber = envelope
             .get("data")
             .get("orderNumber")
             .asText();

            String aggregateId = envelope
             .get("aggregateId")
             .asText();

            if (!orderNumber.equals(aggregateId)) {
                throw new IllegalStateException(
                        "Mismatched order number in event: " + orderNumber
                                + " vs aggregateId: " + aggregateId
                );
            }
            switch(resultEventType)
            {
                // 处理库存预定结果事件成功 
                case INVENTORY_RESERVATION_COMPLETED -> {
                    log.info(
                            "Processing inventory reservation completed command for order: {}, eventId={}",
                            orderNumber,
                            eventId
                    );
                    
                    orderCommandService.markOrderInventoryReservedStatus(eventId, orderNumber);
                }
                // 处理库存预定结果事件失败
                case INVENTORY_RESERVATION_FAILED -> {
                    log.info(
                            "Processing inventory reservation failed command for order: {}, eventId={}",
                            orderNumber,
                            eventId
                    );
                    InventoryReservationFailedEvent command = objectMapper.treeToValue(
                            envelope.get("data"),
                            InventoryReservationFailedEvent.class
                    );
                    
                    orderCommandService.markOrderInventoryFailedStatus(eventId, command);
                }
                // 处理库存释放结果事件成功
                case INVENTORY_RELEASE_COMPLETED -> {
                    log.info(
                            "Processing order cancelled command for order: {}, eventId={}",
                            orderNumber,
                            eventId
                    );
                    InventoryReleaseCompletedEvent command = objectMapper.treeToValue(
                            envelope.get("data"),
                            InventoryReleaseCompletedEvent.class
                    );
                    orderCommandService.markOrderReleaseCompletedStatus(eventId, command);
                }
                // 处理库存过期结果事件成功
                case INVENTORY_EXPIRATION_COMPLETED -> {
                    log.info(
                            "Processing order expired command for order: {}, eventId={}",
                            orderNumber,
                            eventId
                    );
                
                    orderCommandService.markOrderExpiredStatus(eventId, orderNumber);
                }// 处理库存过期结果事件失败
                case INVENTORY_EXPIRATION_FAILED -> {
                    log.info(
                            "Processing order expiration failed command for order: {}, eventId={}",
                            orderNumber,
                            eventId
                    );
                   InventoryExpirationFailedEvent command = objectMapper.treeToValue(
                            envelope.get("data"),
                            InventoryExpirationFailedEvent.class
                    );
                    orderCommandService.markOrderExpiredFailedStatus(eventId, command);
                }
                case INVENTORY_CONFIRMATION_COMPLETED -> {
                    log.info(
                            "Processing order confirmation completed command for order: {}, eventId={}",
                            orderNumber,
                            eventId
                    );
                    orderCommandService.markOrderConfirmedStatus(eventId, orderNumber);
                }
                case INVENTORY_CONFIRMATION_FAILED -> {
                    log.info(
                            "Processing order refunded command for order: {}, eventId={}",
                            orderNumber,
                            eventId
                    );
                    InventoryConfirmationFailedEvent command = objectMapper.treeToValue(
                            envelope.get("data"),
                            InventoryConfirmationFailedEvent.class
                    );
                    orderCommandService.markOrderConfirmationFailedStatus(eventId, command);
                }
                default -> {
                    log.warn(
                            "Received unexpected event type: {} for order: {}, eventId={}",
                            eventType,
                            orderNumber,
                            eventId
                    );
                    return;
                }
            }
        } catch (Exception exception) {
            log.error(
                    "Failed to process inventory reservation command",
                    exception
            );

            // 重新抛出，让 Kafka 知道本次消费失败
            throw new IllegalStateException(
                    "Failed to process order update status command",
                    exception
            );
        }
    }
    
}

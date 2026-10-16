package com.tech.microservices.inventory_service.service.consumer;

import java.util.UUID;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.tech.microservices.inventory_service.dto.command.InventoryReleaseRequestCommand;
import com.tech.microservices.inventory_service.dto.command.InventoryReservationRequestCommand;
import com.tech.microservices.inventory_service.service.InventoryCommandProcessor;
import com.tech.microservices.inventory_service.service.InventoryCommandService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.tech.microservices.inventory_service.model.InventoryCommandType;


@Component 
@RequiredArgsConstructor 
@Slf4j 
public class InventoryCommandConsumer {

// 1. 事件信封示例
// {
//   "eventId": "9cb33c9e-4f19-43d4-aa11-3740fdd7dcf8",
//   "eventType": "INVENTORY_RESERVATION_REQUESTED",
//   "eventVersion": 1,
//   "aggregateType": "ORDER",
//   "aggregateId": "order-publisher-003",
//   "occurredAt": "2026-10-12T10:30:00",
//   "data": {
//     "orderNumber": "order-publisher-003",
//     "items": [
//       {
//         "skuId": "sku-postman-outbox-001",
//         "quantity": 1
//       }
//     ]
//   }
// }
    
    private final ObjectMapper objectMapper;
    //private final InventoryCommandService inventoryCommandService;
    private final InventoryCommandProcessor inventoryCommandProcessor;

    @KafkaListener (
        topics = "${inventory.command.topic}", 
        groupId = "${spring.kafka.consumer.group-id}"
    )

    public void consume(String message)
    {
        try {
            // 1. 解析最外层事件信封
            JsonNode envelope = objectMapper.readTree(message);

            // 2. 获取事件类型
            JsonNode eventType = envelope
                    .get("eventType");

            if (eventType == null || eventType.asText().isBlank()) {
                log.error("Invalid event type");
                throw new IllegalArgumentException("Kafka message has invalid event type");
            }

            String eventTypeStr = eventType.asText();
            // 3. 当前 Consumer 只处理库存预定命令
            InventoryCommandType commandType = InventoryCommandType.valueOf(eventTypeStr);
            
            
            // 4. 获取事件唯一标识
            UUID eventId = UUID.fromString(
                    envelope.get("eventId").asText()
            );
            
            // 5. 获取事件数据
            JsonNode data = envelope.get("data");
            
            // 6. 将事件数据转换为具体的命令对象
            // 监听订单取消或者超时的状态 释放库存流程
            switch (commandType) 
            {
                case InventoryCommandType.INVENTORY_RESERVATION_REQUESTED -> {
                
                     // 5. 只把 data 转换成库存预定命令
                    InventoryReservationRequestCommand command =
                    objectMapper.treeToValue(
                            envelope.get("data"),
                            InventoryReservationRequestCommand.class
                    );

                    log.info(
                    "Received inventory command, eventId={}, orderNumber={}",
                    eventId,
                    command.orderNumber()
                
                );
                  inventoryCommandProcessor.processReservation(
                    eventId,
                    command );
                log.info(
                    "Inventory reservation command completed, eventId={}, orderNumber={}",
                    eventId,
                    command.orderNumber()
                    );
                }
                case InventoryCommandType.INVENTORY_RELEASE_REQUESTED -> {
                    InventoryReleaseRequestCommand command =
                            objectMapper.treeToValue(
                                    data,
                                    InventoryReleaseRequestCommand.class
                            );
                    log.info(
                        "Processing order cancelled command"
                    );
                inventoryCommandProcessor.processOrderCancelled(
                    eventId,
                    command
                    );
                }
                default -> {
                    log.error("Unknown event type: {}", eventTypeStr);
                }
            }
        } catch (Exception exception) {
            log.error(
                    "Failed to process inventory reservation command",
                    exception
            );
            // 重新抛出，让 Kafka 知道本次消费失败
            throw new IllegalStateException(
                    "Failed to process inventory reservation command",
                    exception
            );
        }
    }

}

package com.tech.microservices.inventory_service.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.tech.microservices.inventory_service.dto.InventoryReleaseRequest;
import com.tech.microservices.inventory_service.dto.InventoryReservationRequest;
import com.tech.microservices.inventory_service.dto.event.EventEnvelope;
import com.tech.microservices.inventory_service.dto.event.order.InventoryReleaseCompletedEvent;
import com.tech.microservices.inventory_service.dto.event.order.InventoryReservationCompletedEvent;
import com.tech.microservices.inventory_service.exception.InsufficientInventoryException;
import com.tech.microservices.inventory_service.exception.InvalidReservationStatusException;
import com.tech.microservices.inventory_service.exception.InventoryNotFoundException;
import com.tech.microservices.inventory_service.exception.InventoryReleaseCommandFailureException;
import com.tech.microservices.inventory_service.exception.InventoryReservationCommandFailureException;
import com.tech.microservices.inventory_service.exception.ReservationNotFoundException;
import com.tech.microservices.inventory_service.model.InventoryCommandType;
import com.tech.microservices.inventory_service.model.InventoryOrderEventType;
import com.tech.microservices.inventory_service.dto.command.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;


@Service 
@RequiredArgsConstructor
public class InventoryCommandService {
    
    private final InventoryService inventoryService;
    private final OutboxEventService outboxEventService;
    private final ObjectMapper objectMapper;
    private final ProcessedEventService processedEventService;

    @Transactional
    public void reserveOrderInventory(
        UUID eventID,
        InventoryReservationRequestCommand command) 
    {
        if (!validateReservationCommand(eventID, command)) {
            throw new IllegalArgumentException("Invalid reservation command");
        }
        processedEventService.claim(
            eventID,
            InventoryCommandType.INVENTORY_RESERVATION_REQUESTED,
            command.orderNumber()
        );

        for(InventoryReservationRequestedItem  item: command.items()) {
            String idempotencyKey = eventID.toString() 
            + "-" + command.orderNumber() + "-" + item.skuId();
            
            InventoryReservationRequest request = 
                new InventoryReservationRequest(
                    command.orderNumber(),
                    item.skuId(),
                    item.quantity()
                );
            try{
                  inventoryService.reserveInventory(idempotencyKey, request);
            }catch(InsufficientInventoryException exception)
            {
                throw new InventoryReservationCommandFailureException(
                    command.orderNumber(),
                    item.skuId(),
                    "INSUFFICIENT_INVENTORY",
                    exception.getMessage(),
                    exception
                );
            }catch(InventoryNotFoundException exception)
            {
                throw new InventoryReservationCommandFailureException(
                    command.orderNumber(),
                    item.skuId(),
                    "INVENTORY_NOT_FOUND",
                    exception.getMessage(),
                    exception
                );
            }
            
        }

        // 所有 SKU 成功后，写整体订单级成功事件
        String payload = buildInventoryReservationCompletedPayload(
                eventID,
                command
        );

        outboxEventService.createInventoryReservationCompletedEvent(
                command.orderNumber(),
                payload
        );

        
    }

    // 创建释放完成事件 Create release completed event record
    @Transactional 
    public void cancelOrderInventory(UUID eventID,
        InventoryReleaseRequestCommand command)
    {
        if (!validateReleaseCommand(eventID, command)) {
            throw new IllegalArgumentException("Invalid release command");
        }
         // 保存处理事件
        processedEventService.claim(
        eventID, 
        InventoryCommandType.INVENTORY_RELEASE_REQUESTED,
        command.orderNumber());
        

        for(InventoryReleaseRequestedItem  item: command.items()) {
            String idempotencyKey = eventID.toString() 
            + "-" + command.orderNumber() + "-" + item.skuId();
            
            InventoryReleaseRequest request = 
                new InventoryReleaseRequest(
                    command.orderNumber(),
                    item.skuId(),
                    item.quantity()
                );
            try{
               inventoryService.releaseInventory(idempotencyKey, request);
            }catch(InventoryNotFoundException exception)
            {
                throw new InventoryReleaseCommandFailureException(
                    command.orderNumber(),
                    item.skuId(),
                    "INVENTORY_NOT_FOUND",
                    exception.getMessage(),
                    exception
                );
            }
            catch(ReservationNotFoundException exception)
            {
                throw new InventoryReleaseCommandFailureException(
                    command.orderNumber(),
                    item.skuId(),
                    "RESERVATION_NOT_FOUND",
                    exception.getMessage(),
                    exception
                );
            }
            catch(InsufficientInventoryException exception)
            {
                throw new InventoryReleaseCommandFailureException(
                    command.orderNumber(),
                    item.skuId(),
                    "INSUFFICIENT_INVENTORY",
                    exception.getMessage(),
                    exception
                );
            }
            catch(InvalidReservationStatusException exception)
            {
            
                throw new InventoryReleaseCommandFailureException(
                    command.orderNumber(),
                    item.skuId(),
                    "INVALID_RESERVATION_STATUS",
                    exception.getMessage(),
                    exception
                );

            }
           
        }
        String payload = buildInventoryReleaseCompletedPayload(
                eventID,
                command
            );
        outboxEventService.createInventoryReleaseCompletedEvent(
                command.orderNumber(),
                payload
            );

       
    }
 

    // 创建预定完成事件的记录 Crete reservation completed event
    private String buildInventoryReservationCompletedPayload(
        UUID eventID,
        InventoryReservationRequestCommand command
    ) {
        InventoryReservationCompletedEvent completedEvent = 
            new InventoryReservationCompletedEvent(
                eventID,
                command.orderNumber(),
                command.items(),
                java.time.LocalDateTime.now()
            );
        
        EventEnvelope<InventoryReservationCompletedEvent> envelope = 
            new EventEnvelope<>(
                UUID.randomUUID(),
                InventoryOrderEventType.INVENTORY_RESERVATION_COMPLETED.name(),
                1,
                "ORDER",
                command.orderNumber(),
                LocalDateTime.now(),
                completedEvent
            );
           
        try
        {
            return objectMapper.writeValueAsString(envelope);
        }catch(Exception e)
        {
            throw new IllegalStateException("Failed to serialize InventoryReservationCompletedEvent", e);
        }
        
    }

   // 创建释放完成事件的记录 Create release completed event record
    private String buildInventoryReleaseCompletedPayload(
        UUID eventID,
        InventoryReleaseRequestCommand command
    ) {
        InventoryReleaseCompletedEvent completedEvent = 
            new InventoryReleaseCompletedEvent(
                eventID,
                command.orderNumber(),
                command.reason(),
                command.items(),
                java.time.LocalDateTime.now()
            );
        
        EventEnvelope<InventoryReleaseCompletedEvent> envelope = 
            new EventEnvelope<>(
                UUID.randomUUID(),
                InventoryOrderEventType.INVENTORY_RELEASE_COMPLETED.name(),
                1,
                "ORDER",
                command.orderNumber(),
                LocalDateTime.now(),
                completedEvent
            );

        try
        {
            return objectMapper.writeValueAsString(envelope);
        }catch(Exception e)
        {
            throw new IllegalStateException("Failed to serialize InventoryReleaseCompletedEvent", e);
        }
        
    }

    private boolean validateReservationCommand(
        UUID eventID,
        InventoryReservationRequestCommand command
    )
    {
        // 1. 校验命令基础字段
         if (eventID == null 
            || command == null 
            || command.orderNumber() == null
            || command.orderNumber().isBlank()
            || command.items() == null 
            || command.items().isEmpty()
        )
        {
            return false;
        }

         // 2. 校验每个 SKU
        boolean isValid = command.items().stream().
        allMatch(item -> 
            item != null 
            && item.skuId() != null 
            && !item.skuId().isBlank()
            && item.quantity() != null 
            && item.quantity() > 0
        );

        if (!isValid) {
            return false;
        }

        // 3. 检查同一命令中是否存在重复 SKU
        long distinctSkuCount = command.items().stream()
            .map(InventoryReservationRequestedItem::skuId)
            .distinct()
            .count();
        if (distinctSkuCount != command.items().size()) {
            return false;
        }
        return true;
    }



    private boolean validateReleaseCommand(
        UUID eventID,
        InventoryReleaseRequestCommand command
    )
    {
        // 校验释放命令
        if (eventID == null 
        || command == null 
        || command.orderNumber() == null
        || command.orderNumber().isBlank()
        || command.items() == null
        || command.items().isEmpty()
        )
        {
          return false;
        }
        // 校验每个 SKU
        boolean isValid = command.items().stream().
        allMatch(item -> 
            item != null
            && item.skuId() != null 
            && !item.skuId().isBlank()
            && item.quantity() != null 
            && item.quantity() > 0
        );
        if (!isValid) {
            return false;
        }

        // 检查是否有重复SKU
        long distinctSkuCount = command.items().stream()
            .map(InventoryReleaseRequestedItem::skuId)
            .distinct()
            .count();
        if (distinctSkuCount != command.items().size()) {
            return false;
        }
        return true;
    }
}

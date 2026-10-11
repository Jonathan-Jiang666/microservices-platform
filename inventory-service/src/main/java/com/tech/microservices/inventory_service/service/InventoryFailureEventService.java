package com.tech.microservices.inventory_service.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.apache.kafka.common.errors.IllegalSaslStateException;
import org.springframework.stereotype.Service;

import com.tech.microservices.inventory_service.dto.command.InventoryReleaseRequestCommand;
import com.tech.microservices.inventory_service.dto.command.InventoryReservationRequestCommand;
import com.tech.microservices.inventory_service.dto.event.EventEnvelope;
import com.tech.microservices.inventory_service.dto.event.order.InventoryReleaseFailedEvent;
import com.tech.microservices.inventory_service.dto.event.order.InventoryReservationFailedEvent;
import com.tech.microservices.inventory_service.exception.InventoryReleaseCommandFailureException;
import com.tech.microservices.inventory_service.exception.InventoryReservationCommandFailureException;
import com.tech.microservices.inventory_service.model.InventoryCommandType;
import com.tech.microservices.inventory_service.model.InventoryOrderEventType;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Service 
@RequiredArgsConstructor 
public class InventoryFailureEventService {
    
    private final OutboxEventService outboxEventService;
    private final ObjectMapper objectMapper;
    private final ProcessedEventService processedEventService;

    // propagation 注释表示调用这个方法时，必须开启一个全新的数据库事务；如果外面已有事务，先暂停外部事务
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(UUID eventID, 
        InventoryReservationRequestCommand command,
        InventoryReservationCommandFailureException failureException)
    {
       InventoryReservationFailedEvent failureEvent = new InventoryReservationFailedEvent(
            eventID,
            command.orderNumber(),
            failureException.getFailedSkuId(),
            failureException.getReasonCode(),
            failureException.getMessage(),
            LocalDateTime.now()
        );

        EventEnvelope<InventoryReservationFailedEvent> envelope = new EventEnvelope<>(
            UUID.randomUUID(),
            InventoryOrderEventType.INVENTORY_RESERVATION_FAILED.name(),
            1,
            "ORDER",
            command.orderNumber(),
            LocalDateTime.now(),
            failureEvent
        );

        String payload = null;
        try{
            payload = objectMapper.writeValueAsString(envelope);
        
        }
        catch(Exception e)
        {
            throw new IllegalStateException(
                "Failed to serialize InventoryReservationFailedEvent to JSON", e);
        }
        processedEventService.claim(eventID, 
                InventoryCommandType.INVENTORY_RESERVATION_REQUESTED, command.orderNumber());
        
        outboxEventService.createInventoryReservationFailedEvent(
            command.orderNumber(),
            payload
        );
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordReleaseFailure(UUID eventID, 
        InventoryReleaseRequestCommand command,
        InventoryReleaseCommandFailureException failureException)
    {
       InventoryReleaseFailedEvent failureEvent = new InventoryReleaseFailedEvent(
            eventID,
            command.orderNumber(),
            command.reason(),
            failureException.getFailedSkuId(),
            failureException.getReasonCode(),
            failureException.getMessage(),
            LocalDateTime.now()
        );

        EventEnvelope<InventoryReleaseFailedEvent> envelope = new EventEnvelope<>(
            UUID.randomUUID(),
            InventoryOrderEventType.INVENTORY_RELEASE_FAILED.name(),
            1,
            "ORDER",
            command.orderNumber(),
            LocalDateTime.now(),
            failureEvent
        );

        String payload = null;
        try{
            payload = objectMapper.writeValueAsString(envelope);
            

        }
        catch(Exception e)
        {
            throw new IllegalStateException(
                "Failed to serialize InventoryReleaseFailedEvent to JSON", e);
        }
        processedEventService.claim(eventID, InventoryCommandType.INVENTORY_RELEASE_REQUESTED, command.orderNumber());
        outboxEventService.createInventoryReleaseFailedEvent(
            command.orderNumber(),
            payload
        );
    }
    
}

package com.tech.microservices.inventory_service.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;

import com.tech.microservices.inventory_service.model.InventoryCommandType;
import com.tech.microservices.inventory_service.model.ProcessedEvent;
import com.tech.microservices.inventory_service.repository.ProcessedEventRepository;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import com.tech.microservices.inventory_service.exception.DuplicateCommandEventException;

@Service 
@RequiredArgsConstructor 
public class ProcessedEventService {
    private final JdbcTemplate jdbcTemplate;
    private final ProcessedEventRepository processedEventRepository;

   
   public boolean isEventProcessed(UUID eventId) {
        return processedEventRepository.existsById(eventId);
   }

   @Transactional(propagation = Propagation.MANDATORY)
   public void claim(UUID eventId,
    InventoryCommandType commandType,
    String aggregateId
   )
   {
    try{
          jdbcTemplate.update( """
           INSERT INTO processed_events (event_id, event_type, aggregate_id, processed_at) 
            VALUES (?, ?, ?, ?)
            """,
            eventId.toString(),
            commandType.name(),
            aggregateId,
            LocalDateTime.now());
    }catch(DuplicateKeyException exception)
    {
        throw new DuplicateCommandEventException(eventId, exception);
    }
    //   ProcessedEvent processedEvent = ProcessedEvent.builder()
    //       .eventId(eventId)
    //       .eventType(commandType.name())
    //       .aggregateId(aggregateId)
    //       .processedAt(LocalDateTime.now())
    //       .build();
    //   processedEventRepository.saveAndFlush(processedEvent);
   }

}

package com.tech.microservices.inventory_service.service.publisher;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.tech.microservices.inventory_service.model.OutboxEventStatus;
import com.tech.microservices.inventory_service.model.OutboxEvents;
import com.tech.microservices.inventory_service.repository.OutboxEventRepository;
import com.tech.microservices.inventory_service.service.OutboxEventService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        prefix = "inventory.outbox.publisher",
        name = "enabled",
        havingValue = "true"
)
public class OutboxEventPublisher {
    
    private final OutboxEventService outboxEventService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${inventory.outbox.topic}")
    private String outboxTopic;

    @Scheduled(fixedDelayString = "${inventory.outbox.fixed-delay-ms}")
    public void publishPendingEvents() {
    
        List<OutboxEvents> pendingEvents = outboxEventService.findPendingEvents();
        for(OutboxEvents event : pendingEvents)
        {
            try {
                kafkaTemplate.send(outboxTopic, 
                    event.getAggregateId(), 
                    event.getPayload())
                    .get();
                
                outboxEventService.markPublished(event);
                log.info("OutboxEvent published successfully, this id is {}", event.getId());
            } catch (Exception e) {
                
                outboxEventService.markFailed(event,e);
                log.error("Failed to publish OutboxEvent, this id is {}. Error: {}", event.getId(), e.getMessage());
            }
        }
    }
}

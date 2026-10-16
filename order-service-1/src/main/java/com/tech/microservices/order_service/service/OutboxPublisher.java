package com.tech.microservices.order_service.service;

import com.tech.microservices.order_service.model.OutboxEvents;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        prefix = "order.outbox.publisher",
        name = "enabled",
        havingValue = "true"
)
public class OutboxPublisher {

    private final OutboxEventsService outboxEventsService;

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelayString = "${order.outbox.fixed-delay-ms}")
    public void publishEvents() {

        List<OutboxEvents> pendingEvents = outboxEventsService.findPendingEvents();

        for(OutboxEvents event : pendingEvents)
        {
            try{
                kafkaTemplate.send(
                    event.getDestinationTopic(), 
                    event.getAggregateId(), 
                    event.getPayload())
                    .get();
                outboxEventsService.markPublished(event);
                log.info("Published event id is {}", event.getId());
            }
            catch(Exception e){
                outboxEventsService.markFailed(event,e);
                log.error("Failed to publish event id is {}, error: {}", 
                event.getId(),
                e.getMessage());
            }
        }
    }

}

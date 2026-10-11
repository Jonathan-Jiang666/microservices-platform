package com.example.order.messaging;

import com.example.order.domain.OutboxEvent;
import com.example.order.persistence.OutboxRepository;
import java.util.List;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxPublisher {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisher(OutboxRepository outboxRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${outbox.publisher.delay-ms:1000}")
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> events = outboxRepository.findTop20ByProcessedAtIsNullOrderByIdAsc();
        for (OutboxEvent event : events) {
            kafkaTemplate.send(event.getTopic(), event.getEventKey(), event.getPayload());
            event.markProcessed();
        }
    }
}

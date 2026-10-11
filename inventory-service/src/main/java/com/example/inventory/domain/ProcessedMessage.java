package com.example.inventory.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.Instant;

@Entity
public class ProcessedMessage {

    @Id
    private String messageId;

    private Instant processedAt;

    protected ProcessedMessage() {
    }

    public ProcessedMessage(String messageId) {
        this.messageId = messageId;
        this.processedAt = Instant.now();
    }
}

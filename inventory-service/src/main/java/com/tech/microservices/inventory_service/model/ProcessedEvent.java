package com.tech.microservices.inventory_service.model;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter 
@Table (name = "processed_events")
@Setter 
@Builder 
@Entity 
@NoArgsConstructor 
@AllArgsConstructor 
public class ProcessedEvent {


    @Id
    @Column (name = "event_id", length = 36, nullable = false)
    @JdbcTypeCode (org.hibernate.type.SqlTypes.CHAR)
    private UUID eventId;

    @Column (name = "event_type", length = 100, nullable = false)
    private String eventType;

    @Column (name = "aggregate_id", length = 100, nullable = false)
    private String aggregateId;

    @Column (name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

}

package com.tech.microservices.inventory_service.repository;

import com.tech.microservices.inventory_service.model.OutboxEvents;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvents , UUID> {
    List<OutboxEvents> findTop100ByStatusOrderByCreatedAtAsc(String status);
}

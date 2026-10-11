package com.tech.microservices.inventory_service.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.tech.microservices.inventory_service.model.ProcessedEvent;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID>, JpaSpecificationExecutor<ProcessedEvent> {
    
}

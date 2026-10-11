package com.tech.microservices.order_service.repository;
import com.tech.microservices.order_service.model.OutboxEvents;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvents, UUID> ,JpaSpecificationExecutor<OutboxEvents> {

    List<OutboxEvents> findTop100ByStatusOrderByCreatedAtAsc(String status);


}

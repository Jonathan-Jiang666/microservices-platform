package com.example.order.persistence;

import com.example.order.domain.OutboxEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findTop20ByProcessedAtIsNullOrderByIdAsc();
}

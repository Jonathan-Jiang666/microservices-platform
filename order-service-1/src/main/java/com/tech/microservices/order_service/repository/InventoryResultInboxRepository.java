package com.tech.microservices.order_service.repository;

import com.tech.microservices.order_service.dto.InventoryResultInboxRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class InventoryResultInboxRepository
{
    private final JdbcTemplate jdbcTemplate;

    public void insert(
            String eventId, String eventType,
            String orderNumber, String payloadHash)
    {
        String sql = "INSERT INTO processed_inventory_results (event_id, event_type, order_number, payload_hash) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql, eventId, eventType, orderNumber, payloadHash);
    }

    public Optional<InventoryResultInboxRecord> findByEventId(String eventId) {
        return jdbcTemplate.query("""
            SELECT event_id, event_type, order_number, payload_hash
            FROM processed_inventory_results
            WHERE event_id = ?
            """,
                (rs, rowNum) -> new InventoryResultInboxRecord(
                        rs.getString("event_id"),
                        rs.getString("event_type"),
                        rs.getString("order_number"),
                        rs.getString("payload_hash")),
                eventId
        ).stream().findFirst();
    }

}

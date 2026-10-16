package com.tech.microservices.order_service.service;

import com.tech.microservices.order_service.dto.InventoryResultInboxRecord;
import com.tech.microservices.order_service.integration.inventory.consumer.InventoryResultConsumer;
import com.tech.microservices.order_service.repository.InventoryResultInboxRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static java.util.Objects.hash;

@Service
@RequiredArgsConstructor
public class InventoryResultInboxService {

    private final InventoryResultInboxRepository repository;


    @Transactional
    public void execute(
            String eventId,
            String eventType,
            String orderNumber,
            String payloadHash,
            Runnable action) {
        repository.insert(eventId, eventType, orderNumber, payloadHash);
        action.run();
    }

    @Transactional(readOnly = true)
    public void verifyDuplicate(
            String eventId,
            String eventType,
            String orderNumber,
            String payloadHash
    )
    {
        String normalizedEventType = UUID.fromString(eventId).toString().toUpperCase();
        InventoryResultInboxRecord existing = repository
                .findByEventId(normalizedEventType)
                .orElseThrow(() -> new IllegalStateException("Inbox record missing:" + normalizedEventType));

        //意思是：任意一项不一致，就认为存在冲突。
        //三项都一致，才可以认定是正常重复投递，安全地跳过业务。
        if (!existing.eventType().equals(eventType)
                || !existing.orderNumber().equals(orderNumber)
                || !existing.payloadHash().equals(hash(message)))
        {
            throw new IllegalStateException(
                    "Conflicting inventory result: " + normalizedEventType);
    }
}

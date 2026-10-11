package com.example.inventory.domain;

import com.example.inventory.persistence.InventoryRepository;
import com.example.inventory.persistence.ProcessedMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProcessedMessageRepository processedMessageRepository;

    public InventoryService(InventoryRepository inventoryRepository,
                            ProcessedMessageRepository processedMessageRepository) {
        this.inventoryRepository = inventoryRepository;
        this.processedMessageRepository = processedMessageRepository;
    }

    @Transactional
    public boolean processOrderCreated(String messageId, OrderCreatedEvent event) {
        if (processedMessageRepository.existsById(messageId)) {
            return false;
        }

        processedMessageRepository.save(new ProcessedMessage(messageId));

        inventoryRepository.findById(event.sku()).ifPresent(item -> {
            item.reduce(event.quantity());
            inventoryRepository.save(item);
        });

        return true;
    }
}

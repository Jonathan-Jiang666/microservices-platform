package com.example.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.inventory.domain.InventoryItem;
import com.example.inventory.domain.InventoryService;
import com.example.inventory.domain.OrderCreatedEvent;
import com.example.inventory.persistence.InventoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
class InventoryServiceTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    void processOrderCreatedIsIdempotent() {
        inventoryRepository.save(new InventoryItem("SKU-100", 10));

        boolean first = inventoryService.processOrderCreated("msg-1", new OrderCreatedEvent(1L, "SKU-100", 3));
        boolean second = inventoryService.processOrderCreated("msg-1", new OrderCreatedEvent(1L, "SKU-100", 3));

        assertThat(first).isTrue();
        assertThat(second).isFalse();
        assertThat(inventoryRepository.findById("SKU-100")).get().extracting(InventoryItem::getQuantity).isEqualTo(7);
    }
}

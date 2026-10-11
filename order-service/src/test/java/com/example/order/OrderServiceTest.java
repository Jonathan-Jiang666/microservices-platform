package com.example.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.order.domain.CreateOrderRequest;
import com.example.order.domain.Order;
import com.example.order.domain.OrderService;
import com.example.order.persistence.OutboxRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.kafka.bootstrap-servers=localhost:9092")
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OutboxRepository outboxRepository;

    @Test
    void placeOrderPersistsOutboxEventTransactionally() {
        Order order = orderService.placeOrder(new CreateOrderRequest("SKU-100", 2));

        assertThat(order.getId()).isNotNull();
        assertThat(outboxRepository.findTop20ByProcessedAtIsNullOrderByIdAsc())
                .hasSize(1)
                .first()
                .extracting("eventKey")
                .isEqualTo(String.valueOf(order.getId()));
    }
}

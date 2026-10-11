package com.example.order.domain;

import com.example.order.persistence.OrderRepository;
import com.example.order.persistence.OutboxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;

    public OrderService(OrderRepository orderRepository, OutboxRepository outboxRepository) {
        this.orderRepository = orderRepository;
        this.outboxRepository = outboxRepository;
    }

    @Transactional
    public Order placeOrder(CreateOrderRequest request) {
        Order order = orderRepository.save(new Order(request.sku(), request.quantity()));
        String payload = "{\"orderId\":" + order.getId() + ",\"sku\":\"" + request.sku() + "\",\"quantity\":" + request.quantity() + "}";
        outboxRepository.save(new OutboxEvent("order-events", String.valueOf(order.getId()), payload));
        return order;
    }
}

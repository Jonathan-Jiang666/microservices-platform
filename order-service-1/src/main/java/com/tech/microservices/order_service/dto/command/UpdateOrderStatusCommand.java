package com.tech.microservices.order_service.dto.command;
import com.tech.microservices.order_service.model.OrderStatus;

public record UpdateOrderStatusCommand(
    String orderNumber,
    OrderStatus orderStatus
) {
}
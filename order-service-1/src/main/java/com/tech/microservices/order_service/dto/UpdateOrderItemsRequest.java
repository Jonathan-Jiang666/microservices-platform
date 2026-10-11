package com.tech.microservices.order_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record UpdateOrderItemsRequest(

     @NotEmpty
     @Valid
     List<OrderItemRequest> orderItems
) {
}

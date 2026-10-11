package com.tech.microservices.order_service.dto;

import com.tech.microservices.order_service.model.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(

        @NotNull
        OrderStatus status
) {


}

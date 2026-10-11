package com.tech.microservices.order_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


import java.util.List;

public record OrderRequest(

        @NotBlank
        @Size(max = 64)
        String orderNumber,

        @NotNull
        Long userId,

        @NotBlank
        @Size(max = 3)
        String currency,

        @Size(max = 255)
        String note,

        @Size(max = 255)
        String shippingAddress,

        @Size(max = 255)
        String billingAddress,

        @NotNull
        @Size(max = 100)
        String recipientName,

        @NotNull
        @Size(max = 100)
        String recipientEmail,

        @NotNull
        @Size(max = 20)
        String recipientPhone,

        @NotEmpty
        List<@Valid OrderItemRequest> orderItems
) {
}

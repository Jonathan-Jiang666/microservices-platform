package com.tech.microservices.inventory_service.dto;

import com.tech.microservices.inventory_service.model.IdempotencyOperation;

public record IdempotencyRequestContext(
        IdempotencyOperation operation,  // 这次是什么操作，比如 CREATE_INVENTORY / UPDATE_INVENTORY_STATUS
        String resourceType, //操作什么资源，比如 ORDER
        String resourceId, //资源标识，比如 orderNumber；创建订单时可以为 null
        Object request //请求体，比如 OrderRequest / UpdateOrderStatusRequest
) {
}

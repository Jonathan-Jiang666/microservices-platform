package com.tech.microservices.order_service.dto;



/*
订单不能简单写一个“万能 update”，因为有些字段不能随便改，比如：
totalAmount 不应该前端直接传来改
orderStatus 要校验状态流转
orderItems 改了以后 totalAmount 要重新计算
currency 可能只能在 CREATED 状态改
 */
public record UpdateOrderBasicRequest (
    String shippingAddress,
    String billingAddress,
    String note,
    String recipientName,
    String recipientEmail,
    String recipientPhone
    //String status;
){}

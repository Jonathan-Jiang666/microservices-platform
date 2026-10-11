package com.tech.microservices.order_service.exception;

// 404 Not Found
public class OrderNotFoundException extends RuntimeException{
    public OrderNotFoundException(String orderNumber) {
        super("Order not found with orderNumber: " + orderNumber);
    }
}

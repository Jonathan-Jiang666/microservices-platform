package com.tech.microservices.order_service.model;

import jakarta.persistence.*;
import lombok.*;
import jakarta.persistence.Column;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Builder
@Table(name = "orders")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;           //1

    @Column(name = "order_number", nullable = false, unique = true)
    private String orderNumber; //2


    @Column(name = "user_id", nullable = false)
    private Long userId; //3

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false)
    private OrderStatus orderStatus; //4

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    @DecimalMin(value = "0.00", inclusive = true)
    private BigDecimal totalAmount; //5

    @Column(name = "currency", nullable = false)
    private String currency; //6

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey; //7

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; //8

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt; //9

    @Column(name = "version", nullable = false)
    @Version
    private Long version = 0L; //10

    @Column(name = "note", nullable = true)
    private String note;

    @Column(name = "shipping_address", nullable = false)
    private String shippingAddress;

    @Column(name = "billing_address",nullable = false)
    private String billingAddress;

    @Column(name = "recipient_name",nullable = false)
    private String recipientName;

    @Column(name = "recipient_email",nullable = false)
    private String recipientEmail;

    @Column(name = "recipient_phone",nullable = false)
    private String recipientPhone;

    @PrePersist
    public void onCreate()
    {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate()
    {
        updatedAt = LocalDateTime.now();
    }

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItems> orderItems = new ArrayList<>();
}



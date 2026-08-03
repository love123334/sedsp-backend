package com.example.secdsp.modules.order.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Order status")
public enum OrderStatus {
    PENDING,
    PAID,
    PROCESSING,
    SHIPPING,
    DELIVERED,
    CANCELLED,
    REFUNDED
}
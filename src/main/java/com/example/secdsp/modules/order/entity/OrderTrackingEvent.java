package com.example.secdsp.modules.order.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Order tracking event")
public enum OrderTrackingEvent {
    CREATED,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED_BY_USER,
    CANCELLED_BY_ADMIN,
    PAYMENT_FAILED,
    PAYMENT_SUCCESS
}
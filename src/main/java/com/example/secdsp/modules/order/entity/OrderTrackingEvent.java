package com.example.secdsp.modules.order.entity;

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
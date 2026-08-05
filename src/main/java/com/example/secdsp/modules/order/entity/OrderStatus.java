package com.example.secdsp.modules.order.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    description = """
        Available values:
        - PENDING : Order has been placed but payment is pending.
        - PAID : Payment has been completed successfully.
        - PROCESSING : Order is being prepared.
        - SHIPPING : Order is being shipped.
        - DELIVERED : Order has been delivered successfully.
        - CANCELLED : Order has been cancelled.
        - REFUNDED : Order has been refunded.
        """,
    implementation = OrderStatus.class
)
public enum OrderStatus {
    PENDING,
    PAID,
    PROCESSING,
    SHIPPING,
    DELIVERED,
    CANCELLED,
    REFUNDED
}
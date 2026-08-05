package com.example.secdsp.modules.sellerdashboard.dto;

import com.example.secdsp.modules.order.entity.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Builder
@Schema(
    description = "Recent order information."
)
public record RecentOrderResponse(

    @Schema(description = "Order ID.", example = "1001")
    Long orderId,

    @Schema(description = "Customer name.", example = "John Smith")
    String customer,

    @Schema(description = "Order total amount.", example = "899000")
    BigDecimal total,

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
    OrderStatus status,

    @Schema(
        description = "Order creation timestamp.",
        example = "2026-08-03T14:30:00Z"
    )
    OffsetDateTime createdAt

) {
}
package com.example.secdsp.modules.order.dto.response;

import com.example.secdsp.modules.order.entity.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Order summary")
public class OrderResponse {

    @Schema(example = "1001")
    Long id;

    @Schema(
        description = "Current order status",
        implementation = OrderStatus.class
    )
    OrderStatus status;

    @Schema(example = "4500000")
    BigDecimal subtotal;

    @Schema(example = "30000")
    BigDecimal shippingFee;

    @Schema(example = "100000")
    BigDecimal discount;

    @Schema(example = "4430000")
    BigDecimal total;

    @Schema(example = "2026-08-03T10:15:30+07:00")
    OffsetDateTime createdAt;

    @Schema(description = "Purchased products")
    List<OrderItemResponse> items;
}

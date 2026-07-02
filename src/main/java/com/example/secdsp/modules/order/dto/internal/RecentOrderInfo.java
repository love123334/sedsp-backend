package com.example.secdsp.modules.order.dto.internal;

import com.example.secdsp.modules.order.entity.OrderStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record RecentOrderInfo(
    Long orderId,
    String customer,
    BigDecimal total,
    OrderStatus status,
    LocalDateTime createdAt
) {}
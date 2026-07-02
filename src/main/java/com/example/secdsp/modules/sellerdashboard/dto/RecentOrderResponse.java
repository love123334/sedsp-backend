package com.example.secdsp.modules.sellerdashboard.dto;

import com.example.secdsp.modules.order.entity.OrderStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record RecentOrderResponse(

    Long orderId,

    String customer,

    BigDecimal total,

    OrderStatus status,

    LocalDateTime createdAt

) {}
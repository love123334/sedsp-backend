package com.example.secdsp.modules.order.dto.internal;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record SalesSummaryInfo(

    BigDecimal totalRevenue,

    Long completedOrders,

    BigDecimal averageOrderValue

) {
}

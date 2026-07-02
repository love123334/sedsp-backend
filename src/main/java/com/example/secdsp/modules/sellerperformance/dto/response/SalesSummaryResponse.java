package com.example.secdsp.modules.sellerperformance.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Builder
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SalesSummaryResponse {

    BigDecimal totalRevenue;

    Long completedOrders;

    BigDecimal averageOrderValue;
}

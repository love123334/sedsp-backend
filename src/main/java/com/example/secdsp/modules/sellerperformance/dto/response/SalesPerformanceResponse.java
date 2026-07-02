package com.example.secdsp.modules.sellerperformance.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Builder
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SalesPerformanceResponse {

    SalesSummaryResponse summary;

    List<MonthlyRevenueResponse> monthlyRevenue;

    List<TopProductResponse> topProducts;
}

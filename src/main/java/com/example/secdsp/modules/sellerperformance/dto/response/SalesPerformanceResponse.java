package com.example.secdsp.modules.sellerperformance.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Schema(
    description = "Sales performance information for the authenticated seller."
)
@Builder
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SalesPerformanceResponse {

    @Schema(
        description = "Overall sales summary."
    )
    SalesSummaryResponse summary;

    @Schema(
        description = "Monthly revenue statistics."
    )
    List<MonthlyRevenueResponse> monthlyRevenue;

    @Schema(
        description = "Top-selling products."
    )
    List<TopProductResponse> topProducts;
}

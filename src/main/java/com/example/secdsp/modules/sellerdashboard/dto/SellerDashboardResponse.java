package com.example.secdsp.modules.sellerdashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
@Schema(
    description = "Comprehensive dashboard information for the authenticated seller."
)
public record SellerDashboardResponse(

    @Schema(
        description = "Revenue statistics."
    )
    RevenueSummary revenue,

    @Schema(
        description = "Order statistics grouped by status."
    )
    OrderSummary orders,

    @Schema(
        description = "Product statistics."
    )
    ProductSummary products,

    @Schema(
        description = "Inventory overview."
    )
    InventorySummary inventory,

    @Schema(
        description = "Recently received orders."
    )
    List<RecentOrderResponse> recentOrders,

    @Schema(
        description = "Products that are running low on stock."
    )
    List<LowStockProductResponse> lowStockProducts,

    @Schema(
        description = "Actionable recommendations generated from seller dashboard analytics."
    )
    List<RecommendationResponse> recommendations,

    @Schema(
        description = "Seller rating statistics and recent customer reviews."
    )
    SellerRatingSummary rating
) {
}
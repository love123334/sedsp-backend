package com.example.secdsp.modules.sellerdashboard.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record SellerDashboardResponse(

    RevenueSummary revenue,

    OrderSummary orders,

    ProductSummary products,

    InventorySummary inventory,

    List<RecentOrderResponse> recentOrders,

    List<LowStockProductResponse> lowStockProducts,

    List<String> recommendations
) {}
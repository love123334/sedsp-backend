package com.example.secdsp.modules.platformrevenue.dto.response;

import com.example.secdsp.modules.order.entity.OrderStatus;
import com.example.secdsp.modules.payment.entity.PaymentMethod;
import com.example.secdsp.modules.platformrevenue.dto.request.RevenueGranularity;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record PlatformRevenueDashboardResponse(

    Period period,

    Overview overview,

    List<OrderStatusDistributionItem> orderStatusDistribution,

    List<RevenueTrendPoint> revenueTrend,

    List<TopSellerItem> topSellers,

    List<TopProductItem> topProducts,

    List<TopCategoryItem> topCategories,

    List<PaymentMethodDistributionItem> paymentMethodDistribution,

    PlatformActivity platformActivity,

    List<PlatformActivityTrendPoint> activityTrend
) {

    @Builder
    public record Period(
        LocalDate fromDate,
        LocalDate toDate,
        RevenueGranularity granularity,
        LocalDateTime generatedAt
    ) {}

    @Builder
    public record Overview(
        BigDecimal grossMerchandiseValue,
        BigDecimal previousPeriodGmv,
        BigDecimal gmvGrowthPercentage,
        BigDecimal successfulPaymentAmount,
        BigDecimal deliveredOrderValue,
        BigDecimal totalDiscountAmount,
        BigDecimal totalShippingFee,
        Long totalOrders,
        Long deliveredOrders,
        BigDecimal averageOrderValue,
        Long unitsSold,
        Long activeSellerCount,
        Long activeCustomerCount
    ) {}

    @Builder
    public record OrderStatusDistributionItem(
        OrderStatus status,
        Long orderCount,
        BigDecimal percentage
    ) {}

    @Builder
    public record RevenueTrendPoint(
        LocalDate periodStart,
        BigDecimal grossMerchandiseValue,
        BigDecimal deliveredOrderValue,
        Long deliveredOrders,
        Long unitsSold
    ) {}

    @Builder
    public record TopSellerItem(
        Long sellerId,
        String sellerName,
        BigDecimal grossMerchandiseValue,
        Long deliveredOrders,
        Long unitsSold,
        BigDecimal marketSharePercentage
    ) {}

    @Builder
    public record TopProductItem(
        Long productId,
        String productName,
        Long sellerId,
        String sellerName,
        Long deliveredOrders,
        Long unitsSold,
        BigDecimal grossMerchandiseValue
    ) {}

    @Builder
    public record TopCategoryItem(
        Long categoryId,
        String categoryName,
        Long deliveredOrders,
        Long unitsSold,
        BigDecimal grossMerchandiseValue,
        BigDecimal marketSharePercentage
    ) {}

    @Builder
    public record PaymentMethodDistributionItem(
        PaymentMethod paymentMethod,
        Long totalPaymentCount,
        Long successfulPaymentCount,
        Long pendingPaymentCount,
        Long failedPaymentCount,
        BigDecimal successfulAmount,
        BigDecimal percentage
    ) {}

    @Builder
    public record PlatformActivity(
        Long totalSellers,
        Long activeSellerAccounts,
        Long newSellers,
        Long totalCustomers,
        Long activeCustomerAccounts,
        Long newCustomers,
        Long totalProducts,
        Long activeProducts,
        Long inactiveProducts,
        Long outOfStockProducts,
        Long newProducts,
        Long totalCategories,
        Long uncategorizedProducts
    ) {}

    @Builder
    public record PlatformActivityTrendPoint(
        LocalDate periodStart,
        Long newSellers,
        Long newCustomers,
        Long newProducts
    ) {}
}

package com.example.secdsp.modules.sellerperformance.service;

import com.example.secdsp.common.exception.UnauthorizedException;
import com.example.secdsp.common.util.SecurityUtils;
import com.example.secdsp.modules.order.dto.internal.MonthlyRevenueInfo;
import com.example.secdsp.modules.order.dto.internal.SalesSummaryInfo;
import com.example.secdsp.modules.order.dto.internal.TopProductSalesInfo;
import com.example.secdsp.modules.order.service.OrderService;
import com.example.secdsp.modules.payment.service.PaymentService;
import com.example.secdsp.modules.sellerperformance.dto.response.MonthlyRevenueResponse;
import com.example.secdsp.modules.sellerperformance.dto.response.SalesPerformanceResponse;
import com.example.secdsp.modules.sellerperformance.dto.response.SalesSummaryResponse;
import com.example.secdsp.modules.sellerperformance.dto.response.TopProductResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SellerPerformanceServiceImpl
    implements SellerPerformanceService {

    private final PaymentService paymentService;
    private final OrderService orderService;

    @Override
    public SalesPerformanceResponse getPerformance() {

        Long sellerId = SecurityUtils.getCurrentUserId();

        if (sellerId == null) {
            throw new UnauthorizedException("Authentication required.");
        }

        log.info("Loading sales performance for seller {}", sellerId);

        SalesSummaryInfo summaryInfo =
            paymentService.getSalesSummary(sellerId);

        List<MonthlyRevenueInfo> monthlyInfos =
            paymentService.getMonthlyRevenue(sellerId);

        List<TopProductSalesInfo> topProductInfos =
            orderService.getTopSellingProducts(sellerId);

        SalesSummaryResponse summary =
            SalesSummaryResponse.builder()
                .totalRevenue(summaryInfo.totalRevenue())
                .completedOrders(summaryInfo.completedOrders())
                .averageOrderValue(summaryInfo.averageOrderValue())
                .build();

        List<MonthlyRevenueResponse> monthlyRevenue =
            monthlyInfos.stream()
                .map(info ->
                         MonthlyRevenueResponse.builder()
                             .month(info.month())
                             .revenue(info.revenue())
                             .build()
                )
                .toList();

        List<TopProductResponse> topProducts =
            topProductInfos.stream()
                .map(info ->
                         TopProductResponse.builder()
                             .productId(info.productId())
                             .productName(info.productName())
                             .quantitySold(info.quantitySold())
                             .revenue(info.revenue())
                             .build()
                )
                .toList();

        return SalesPerformanceResponse.builder()
            .summary(summary)
            .monthlyRevenue(monthlyRevenue)
            .topProducts(topProducts)
            .build();
    }
}

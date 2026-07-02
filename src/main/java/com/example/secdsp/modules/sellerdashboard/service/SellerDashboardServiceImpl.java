package com.example.secdsp.modules.sellerdashboard.service;

import com.example.secdsp.common.exception.UnauthorizedException;
import com.example.secdsp.common.util.SecurityUtils;
import com.example.secdsp.modules.inventory.dto.internal.InventorySummaryInfo;
import com.example.secdsp.modules.inventory.service.InventoryService;
import com.example.secdsp.modules.order.dto.internal.OrderDashboardInfo;
import com.example.secdsp.modules.order.dto.internal.RecentOrderInfo;
import com.example.secdsp.modules.order.dto.internal.RevenueInfo;
import com.example.secdsp.modules.order.service.OrderService;
import com.example.secdsp.modules.order.service.PaymentService;
import com.example.secdsp.modules.product.dto.internal.LowStockProductInfo;
import com.example.secdsp.modules.product.dto.internal.ProductSummaryInfo;
import com.example.secdsp.modules.product.service.ProductService;
import com.example.secdsp.modules.sellerdashboard.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SellerDashboardServiceImpl
    implements SellerDashboardService {

    private final ProductService productService;
    private final OrderService orderService;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;

    @Override
    public SellerDashboardResponse getDashboard() {

        Long sellerId = SecurityUtils.getCurrentUserId();

        if (sellerId == null) {
            throw new UnauthorizedException("Authentication required.");
        }

        log.info("Loading dashboard for seller {}", sellerId);

        RevenueInfo revenueInfo =
            paymentService.getRevenue(sellerId);

        RevenueSummary revenue =
            RevenueSummary.builder()
                .totalRevenue(revenueInfo.totalRevenue())
                .completedOrders(revenueInfo.completedOrders())
                .build();

        // ✅ Orders summary
        OrderDashboardInfo orderInfo =
            orderService.getSellerOrderSummary(sellerId);

        OrderSummary orderSummary =
            OrderSummary.builder()
                .pending(orderInfo.pending())
                .processing(orderInfo.processing())
                .shipping(orderInfo.shipping())
                .delivered(orderInfo.delivered())
                .build();

        // ✅ Product summary
        ProductSummaryInfo productInfo =
            productService.getSellerProductSummary(sellerId);

        ProductSummary productSummary =
            ProductSummary.builder()
                .totalProducts(productInfo.totalProducts())
                .activeProducts(productInfo.activeProducts())
                .build();

        // ✅ Inventory summary
        InventorySummaryInfo inventoryInfo =
            inventoryService.getInventorySummary(sellerId);

        InventorySummary inventorySummary =
            InventorySummary.builder()
                .lowStockProducts(inventoryInfo.lowStockProducts())
                .outOfStockProducts(inventoryInfo.outOfStockProducts())
                .build();

        // ✅ Recent Orders
        List<RecentOrderInfo> recentOrderInfos =
            orderService.getRecentOrders(sellerId);

        List<RecentOrderResponse> recentOrders =
            recentOrderInfos.stream()
                .map(info ->
                         RecentOrderResponse.builder()
                             .orderId(info.orderId())
                             .customer(info.customer())
                             .total(info.total())
                             .status(info.status())
                             .createdAt(info.createdAt())
                             .build()
                ).toList();

        // ✅ Low stock products
        List<LowStockProductInfo> lowStockInfos =
            inventoryService.getLowStockProducts(sellerId);

        List<LowStockProductResponse> lowStockProducts =
            lowStockInfos.stream()
                .map(info ->
                         LowStockProductResponse.builder()
                             .productId(info.productId())
                             .productName(info.productName())
                             .quantity(info.quantity())
                             .build()
                ).toList();

        return SellerDashboardResponse.builder()
            .revenue(revenue)
            .orders(orderSummary)
            .products(productSummary)
            .inventory(inventorySummary)
            .recentOrders(recentOrders)
            .lowStockProducts(lowStockProducts)
            .recommendations(Collections.emptyList())
            .build();
    }
}
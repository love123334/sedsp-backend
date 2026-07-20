package com.example.secdsp.modules.sellerdashboard.service;

import com.example.secdsp.common.exception.UnauthorizedException;
import com.example.secdsp.common.util.SecurityUtils;
import com.example.secdsp.modules.inventory.dto.internal.InventorySummaryInfo;
import com.example.secdsp.modules.inventory.service.InventoryService;
import com.example.secdsp.modules.order.dto.internal.OrderDashboardInfo;
import com.example.secdsp.modules.order.dto.internal.RevenueInfo;
import com.example.secdsp.modules.order.service.OrderService;
import com.example.secdsp.modules.order.service.PaymentService;
import com.example.secdsp.modules.product.dto.internal.ProductSummaryInfo;
import com.example.secdsp.modules.product.service.ProductService;
import com.example.secdsp.modules.review.dto.response.RatingBreakdownItem;
import com.example.secdsp.modules.review.dto.response.RecentReviewResponse;
import com.example.secdsp.modules.review.repository.ProductReviewRepository;
import com.example.secdsp.modules.sellerdashboard.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class SellerDashboardServiceImpl implements SellerDashboardService {

    private final ProductService productService;
    private final OrderService orderService;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;
    private final ProductReviewRepository reviewRepository;

    @Override
    public SellerDashboardResponse getDashboard() {

        Long sellerId = getCurrentSellerId();

        log.info("Loading dashboard for seller {}", sellerId);

        return SellerDashboardResponse.builder()
            .revenue(buildRevenueSummary(sellerId))
            .orders(buildOrderSummary(sellerId))
            .products(buildProductSummary(sellerId))
            .inventory(buildInventorySummary(sellerId))
            .recentOrders(buildRecentOrders(sellerId))
            .lowStockProducts(buildLowStockProducts(sellerId))
            .rating(buildRatingSection(sellerId))
            .recommendations(Collections.emptyList())
            .build();
    }

    // ==============================
    // PRIVATE METHODS
    // ==============================

    private Long getCurrentSellerId() {
        Long sellerId = SecurityUtils.getCurrentUserId();
        if (sellerId == null) {
            throw new UnauthorizedException("Authentication required.");
        }
        return sellerId;
    }

    private RevenueSummary buildRevenueSummary(Long sellerId) {

        RevenueInfo info = paymentService.getRevenue(sellerId);

        return RevenueSummary.builder()
            .totalRevenue(info.totalRevenue())
            .completedOrders(info.completedOrders())
            .build();
    }

    private OrderSummary buildOrderSummary(Long sellerId) {

        OrderDashboardInfo info =
            orderService.getSellerOrderSummary(sellerId);

        return OrderSummary.builder()
            .pending(info.pending())
            .processing(info.processing())
            .shipping(info.shipping())
            .delivered(info.delivered())
            .build();
    }

    private ProductSummary buildProductSummary(Long sellerId) {

        ProductSummaryInfo info =
            productService.getSellerProductSummary(sellerId);

        return ProductSummary.builder()
            .totalProducts(info.totalProducts())
            .activeProducts(info.activeProducts())
            .build();
    }

    private InventorySummary buildInventorySummary(Long sellerId) {

        InventorySummaryInfo info =
            inventoryService.getInventorySummary(sellerId);

        return InventorySummary.builder()
            .lowStockProducts(info.lowStockProducts())
            .outOfStockProducts(info.outOfStockProducts())
            .build();
    }

    private List<RecentOrderResponse> buildRecentOrders(Long sellerId) {

        return orderService.getRecentOrders(sellerId)
            .stream()
            .map(info -> RecentOrderResponse.builder()
                .orderId(info.orderId())
                .customer(info.customer())
                .total(info.total())
                .status(info.status())
                .createdAt(info.createdAt())
                .build())
            .toList();
    }

    private List<LowStockProductResponse> buildLowStockProducts(Long sellerId) {

        return inventoryService.getLowStockProducts(sellerId)
            .stream()
            .map(info -> LowStockProductResponse.builder()
                .productId(info.productId())
                .productName(info.productName())
                .quantity(info.quantity())
                .build())
            .toList();
    }

    private SellerRatingSummary buildRatingSection(Long sellerId) {

        Object[] summary = reviewRepository.getSellerRatingSummary(sellerId);

        Double avg = summary[0] != null ? (Double) summary[0] : 0.0;
        Long total = summary[1] != null ? (Long) summary[1] : 0L;

        List<Object[]> rawBreakdown =
            reviewRepository.getSellerRatingBreakdown(sellerId);

        Map<Integer, Long> countMap = new HashMap<>();
        for (Object[] row : rawBreakdown) {
            countMap.put((Integer) row[0], (Long) row[1]);
        }

        List<RatingBreakdownItem> breakdown = new ArrayList<>();

        for (int i = 5; i >= 1; i--) {
            Long count = countMap.getOrDefault(i, 0L);
            double percent = total == 0 ? 0 : (count * 100.0) / total;

            breakdown.add(new RatingBreakdownItem(
                i,
                count,
                Math.round(percent * 100.0) / 100.0
            ));
        }

        List<RecentReviewResponse> recentReviews =
            reviewRepository
                .findTop5ByProduct_Seller_IdOrderByCreatedAtDesc(sellerId)
                .stream()
                .map(r -> new RecentReviewResponse(
                    r.getId(),
                    r.getProduct().getId(),
                    r.getProduct().getName(),
                    r.getRating(),
                    r.getComment(),
                    r.getCreatedAt()
                ))
                .toList();

        String warning = null;

        if (avg < 3.5 && total >= 5) {
            warning = "Your shop rating is below average. Please improve service quality.";
        }

        return SellerRatingSummary.builder()
            .averageRating(avg)
            .totalReviews(total)
            .ratingBreakdown(breakdown)
            .recentReviews(recentReviews)
            .warning(warning)
            .build();
    }
}
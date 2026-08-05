package com.example.secdsp.modules.sellerdashboard.service;

import com.example.secdsp.common.exception.UnauthorizedException;
import com.example.secdsp.common.util.SecurityUtils;
import com.example.secdsp.modules.inventory.dto.internal.InventorySummaryInfo;
import com.example.secdsp.modules.inventory.service.InventoryService;
import com.example.secdsp.modules.order.dto.internal.OrderDashboardInfo;
import com.example.secdsp.modules.order.dto.internal.RevenueInfo;
import com.example.secdsp.modules.order.service.OrderService;
import com.example.secdsp.modules.payment.service.PaymentService;
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

        SellerRatingSummary rating = buildRatingSection(sellerId);
        OrderSummary orders = buildOrderSummary(sellerId);
        InventorySummary inventory = buildInventorySummary(sellerId);
        List<LowStockProductResponse> lowStock = buildLowStockProducts(sellerId);

        return SellerDashboardResponse.builder()
            .revenue(buildRevenueSummary(sellerId))
            .orders(orders)
            .products(buildProductSummary(sellerId))
            .inventory(inventory)
            .recentOrders(buildRecentOrders(sellerId))
            .lowStockProducts(lowStock)
            .rating(rating)
            .averageRating(rating.averageRating())
            .totalReviews(rating.totalReviews())
            .ratingBreakdown(rating.ratingBreakdown())
            .recentReviews(rating.recentReviews())
            .ratingWarning(rating.warning())
            .recommendations(buildRecommendations(inventory, lowStock, rating, orders))
            .build();
    }

    /** Actionable DSS-style tips from live dashboard metrics (not generic placeholders). */
    private List<String> buildRecommendations(
        InventorySummary inventory,
        List<LowStockProductResponse> lowStock,
        SellerRatingSummary rating,
        OrderSummary orders
    ) {
        List<String> tips = new ArrayList<>();

        if (inventory.outOfStockProducts() > 0) {
            tips.add(
                "Có " + inventory.outOfStockProducts()
                    + " sản phẩm hết hàng — mở DSS → Khuyến nghị tồn kho để lập lệnh nhập."
            );
        }
        if (!lowStock.isEmpty()) {
            String names = lowStock.stream()
                .limit(2)
                .map(LowStockProductResponse::productName)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
            tips.add(
                "Tồn thấp: " + names
                    + (lowStock.size() > 2 ? " (+" + (lowStock.size() - 2) + ")" : "")
                    + ". Chạy dự báo nhu cầu trước khi nhập."
            );
        }
        if (orders.pending() > 0) {
            tips.add(
                "Có " + orders.pending()
                    + " đơn chờ xử lý — ưu tiên xác nhận để tránh hủy đơn."
            );
        }
        if (rating.warning() != null && !rating.warning().isBlank()) {
            tips.add(rating.warning() + " Xem đánh giá gần đây và cải thiện phản hồi.");
        }
        if (orders.shipping() > 5) {
            tips.add(
                "Có " + orders.shipping()
                    + " đơn đang giao — theo dõi tracking để giảm khiếu nại giao hàng."
            );
        }
        if (tips.isEmpty()) {
            tips.add(
                "Shop đang ổn định. Dùng DSS → Gợi ý giá / What-if để tối ưu biên lợi nhuận."
            );
        }
        return tips.stream().limit(5).toList();
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
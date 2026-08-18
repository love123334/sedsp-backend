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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        RevenueSummary revenue = buildRevenueSummary(sellerId);
        OrderSummary orders = buildOrderSummary(sellerId);
        ProductSummary products = buildProductSummary(sellerId);
        InventorySummary inventory = buildInventorySummary(sellerId);
        List<RecentOrderResponse> recentOrders = buildRecentOrders(sellerId);
        List<LowStockProductResponse> lowStockProducts = buildLowStockProducts(sellerId);
        SellerRatingSummary rating = buildRatingSection(sellerId);

        // Sinh danh sách gợi ý nâng cấp theo DTO
        List<RecommendationResponse> recommendations = buildRecommendations(
            revenue, orders, products, inventory, rating, lowStockProducts
        );

        return SellerDashboardResponse.builder()
            .revenue(revenue)
            .orders(orders)
            .products(products)
            .inventory(inventory)
            .recentOrders(recentOrders)
            .lowStockProducts(lowStockProducts)
            .rating(rating)
            .recommendations(recommendations)
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
                "Shop đang ổn định. Dùng DSS → Gợi ý giá nâng cao / Hiệu quả đơn hàng để tối ưu biên lợi nhuận."
            );
        }
        return tips.stream().limit(5).toList();
    }

    // ==============================
    // RULE ENGINE: RECOMMENDATIONS
    // ==============================

    private List<RecommendationResponse> buildRecommendations(
        RevenueSummary revenue,
        OrderSummary orders,
        ProductSummary products,
        InventorySummary inventory,
        SellerRatingSummary rating,
        List<LowStockProductResponse> lowStockProducts
    ) {
        List<RecommendationResponse> list = new ArrayList<>();

        // 1. CẢNH BÁO ĐƠN HÀNG
        if (orders != null && orders.pending() > 0) {
            list.add(RecommendationResponse.builder()
                         .id("REC_PENDING_ORDERS")
                         .title("Xử lý đơn hàng mới")
                         .message(String.format(
                             "Bạn có %d đơn hàng đang chờ xác nhận. Hãy đóng gói sớm để không trễ hạn delivery!",
                             orders.pending()
                         ))
                         .priority(RecommendationPriority.HIGH)
                         .actionUrl("/seller/orders?status=PENDING")
                         .actionLabel("Xem đơn hàng")
                         .build());
        }

        // 2. CẢNH BÁO TỒN KHO HẾT HÀNG
        if (inventory != null && inventory.outOfStockProducts() > 0) {
            list.add(RecommendationResponse.builder()
                         .id("REC_OUT_OF_STOCK")
                         .title("Sản phẩm hết hàng")
                         .message(String.format(
                             "Hiện tại có %d sản phẩm đã hết hàng. Hãy cập nhật kho để tiếp tục bán hàng.",
                             inventory.outOfStockProducts()
                         ))
                         .priority(RecommendationPriority.HIGH)
                         .actionUrl("/seller/inventory?filter=OUT_OF_STOCK")
                         .actionLabel("Cập nhật kho")
                         .build());
        }

        // 3. CẢNH BÁO SẮP HẾT HÀNG
        if (inventory != null && inventory.lowStockProducts() > 0) {
            String sampleName = (lowStockProducts != null && !lowStockProducts.isEmpty())
                ? "'" + lowStockProducts.get(0).productName() + "' "
                : "";
            list.add(RecommendationResponse.builder()
                         .id("REC_LOW_STOCK")
                         .title("Tồn kho mức cảnh báo")
                         .message(String.format(
                             "Sản phẩm %ssắp hết hàng. Tổng cộng %d sản phẩm cần bổ sung.",
                             sampleName,
                             inventory.lowStockProducts()
                         ))
                         .priority(RecommendationPriority.MEDIUM)
                         .actionUrl("/seller/inventory?filter=LOW_STOCK")
                         .actionLabel("Nhập thêm hàng")
                         .build());
        }

        // 4. CẢNH BÁO UY TÍN & RATING
        if (rating != null && rating.warning() != null && !rating.warning().isBlank()) {
            list.add(RecommendationResponse.builder()
                         .id("REC_RATING_WARNING")
                         .title("Cảnh báo chất lượng Shop")
                         .message(rating.warning())
                         .priority(RecommendationPriority.HIGH)
                         .actionUrl("/seller/reviews")
                         .actionLabel("Xem đánh giá")
                         .build());
        }

        // 5. CẢNH BÁO SẢN PHẨM
        if (products != null) {
            if (products.totalProducts() == 0) {
                list.add(RecommendationResponse.builder()
                             .id("REC_NO_PRODUCTS")
                             .title("Bắt đầu kinh doanh")
                             .message("Gian hàng của bạn chưa có sản phẩm nào. Hãy đăng sản phẩm đầu tiên!")
                             .priority(RecommendationPriority.HIGH)
                             .actionUrl("/seller/products/create")
                             .actionLabel("Đăng sản phẩm")
                             .build());
            }
        }

        // 6. TRƯỜNG HỢP MẶC ĐỊNH
        if (list.isEmpty()) {
            list.add(RecommendationResponse.builder()
                         .id("REC_ALL_GOOD")
                         .title("Vận hành tuyệt vời!")
                         .message("Gian hàng của bạn đang hoạt động rất tốt. Không có cảnh báo nào cần xử lý ngay.")
                         .priority(RecommendationPriority.INFO)
                         .actionUrl("/seller/analytics")
                         .actionLabel("Xem phân tích")
                         .build());
        }

        return list;
    }

    // ==============================
    // PRIVATE BUILDER METHODS
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
            .growthRate(info.growthRate())
            .build();
    }

    private OrderSummary buildOrderSummary(Long sellerId) {
        OrderDashboardInfo info = orderService.getSellerOrderSummary(sellerId);
        return OrderSummary.builder()
            .pending(info.pending())
            .processing(info.processing())
            .shipping(info.shipping())
            .delivered(info.delivered())
            .build();
    }

    private ProductSummary buildProductSummary(Long sellerId) {
        ProductSummaryInfo info = productService.getSellerProductSummary(sellerId);
        return ProductSummary.builder()
            .totalProducts(info.totalProducts())
            .activeProducts(info.activeProducts())
            .build();
    }

    private InventorySummary buildInventorySummary(Long sellerId) {
        InventorySummaryInfo info = inventoryService.getInventorySummary(sellerId);
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
        List<Object[]> summaryRows = reviewRepository
            .getSellerRatingSummary(sellerId);
        Object[] summary = summaryRows == null || summaryRows.isEmpty()
            ? new Object[] { null, 0L }
            : summaryRows.get(0);

        double avg = toDouble(summary != null && summary.length > 0 ? summary[0] : null);
        long total = toLong(summary != null && summary.length > 1 ? summary[1] : null);

        List<Object[]> rawBreakdown = reviewRepository.getSellerRatingBreakdown(sellerId);

        Map<Integer, Long> countMap = new HashMap<>();
        if (rawBreakdown != null) {
            for (Object[] row : rawBreakdown) {
                if (row == null || row.length < 2) continue;
                countMap.put(toInt(row[0]), toLong(row[1]));
            }
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

        List<RecentReviewResponse> recentReviews = reviewRepository
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
            warning = "Đánh giá trung bình của gian hàng đang thấp hơn 3.5⭐. Vui lòng cải thiện dịch vụ.";
        }

        return SellerRatingSummary.builder()
            .averageRating(avg)
            .totalReviews(total)
            .ratingBreakdown(breakdown)
            .recentReviews(recentReviews)
            .warning(warning)
            .build();
    }

    private static double toDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number number) return number.doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException ex) {
            return 0.0;
        }
    }

    private static long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number number) return number.longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private static int toInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Number number) return number.intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}

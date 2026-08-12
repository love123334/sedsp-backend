package com.example.secdsp.modules.dss.service;

import com.example.secdsp.modules.dss.dto.response.DssProductContextResponse;
import com.example.secdsp.modules.order.repository.OrderItemRepository;
import com.example.secdsp.modules.product.dto.internal.PriceHistoryInfo;
import com.example.secdsp.modules.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DssProductContextService {

    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;

    public DssProductContextResponse buildContext(
        Long productId,
        Long sellerId,
        LocalDate rangeStart,
        LocalDate rangeEnd,
        LocalDate firstSaleDate,
        List<PriceHistoryInfo> priceHistoriesInRange
    ) {
        OffsetDateTime listedAt = productRepository.findById(productId)
            .map(p -> p.getCreatedAt())
            .orElse(null);

        int daysListed = listedAt == null
            ? 0
            : (int) ChronoUnit.DAYS.between(listedAt.toLocalDate(), LocalDate.now()) + 1;

        Integer daysSinceFirstSale = firstSaleDate == null
            ? null
            : (int) ChronoUnit.DAYS.between(firstSaleDate, LocalDate.now()) + 1;

        int priceChangeCount = priceHistoriesInRange == null ? 0 : priceHistoriesInRange.size();

        List<Object[]> ranking = orderItemRepository.findSellerProductSalesRanking(
            sellerId,
            rangeStart.atStartOfDay(),
            rangeEnd.plusDays(1).atStartOfDay()
        );

        int shopProductCount = ranking.size();
        int rank = 0;
        long productQty = 0;
        for (int i = 0; i < ranking.size(); i++) {
            Long pid = ((Number) ranking.get(i)[0]).longValue();
            long qty = ((Number) ranking.get(i)[1]).longValue();
            if (pid.equals(productId)) {
                rank = i + 1;
                productQty = qty;
                break;
            }
        }

        String tier = resolveTier(rank, shopProductCount, productQty);
        String summary = buildPerformanceSummary(
            rank,
            shopProductCount,
            tier,
            productQty,
            priceChangeCount,
            daysListed
        );

        return DssProductContextResponse.builder()
            .listedAt(listedAt)
            .daysListed(daysListed)
            .firstSaleDate(firstSaleDate)
            .daysSinceFirstSale(daysSinceFirstSale)
            .priceChangeCount(priceChangeCount)
            .shopSalesRank(rank > 0 ? rank : null)
            .shopProductCount(shopProductCount > 0 ? shopProductCount : null)
            .performanceTier(tier)
            .performanceSummary(summary)
            .build();
    }

    private static String resolveTier(int rank, int total, long qty) {
        if (total <= 1) {
            return "ONLY";
        }
        if (rank <= 0 || qty <= 0) {
            return "LOW";
        }
        double percentile = (double) rank / total;
        if (percentile <= 0.25) {
            return "TOP";
        }
        if (percentile >= 0.75) {
            return "LOW";
        }
        return "MID";
    }

    private static String buildPerformanceSummary(
        int rank,
        int total,
        String tier,
        long qty,
        int priceChanges,
        int daysListed
    ) {
        String rankPart = rank > 0 && total > 0
            ? String.format("Xếp hạng #%d / %d SP trong shop (đã bán %d SP trong kỳ).", rank, total, qty)
            : "Chưa có đơn giao thành công trong kỳ phân tích.";
        String tierVi = switch (tier) {
            case "TOP" -> "Nhóm bán chạy";
            case "MID" -> "Nhóm trung bình";
            case "LOW" -> "Nhóm bán chậm";
            default -> "Sản phẩm duy nhất / mới";
        };
        return String.format(
            "%s %s. Đăng shop %d ngày; %d lần chỉnh giá trong kỳ.",
            tierVi,
            rankPart,
            daysListed,
            priceChanges
        );
    }
}

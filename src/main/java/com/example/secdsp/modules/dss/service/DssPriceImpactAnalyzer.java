package com.example.secdsp.modules.dss.service;

import com.example.secdsp.modules.dss.dto.response.DssPriceChangeImpactResponse;
import com.example.secdsp.modules.product.dto.internal.PriceHistoryInfo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Đo tác động chỉnh giá: so sánh TB bán/ngày trước vs sau mỗi lần đổi giá.
 */
public final class DssPriceImpactAnalyzer {

    private static final int WINDOW_DAYS = 7;
    private static final int SCALE = 2;

    private DssPriceImpactAnalyzer() {
    }

    public static List<DssPriceChangeImpactResponse> analyze(
        List<PriceHistoryInfo> priceHistories,
        Map<LocalDate, Long> dailySales,
        LocalDate rangeStart,
        LocalDate rangeEnd
    ) {
        if (priceHistories == null || priceHistories.isEmpty()) {
            return List.of();
        }

        List<PriceHistoryInfo> sorted = priceHistories.stream()
            .sorted(Comparator.comparing(PriceHistoryInfo::changedAt))
            .toList();

        List<DssPriceChangeImpactResponse> out = new ArrayList<>();
        for (PriceHistoryInfo ph : sorted) {
            LocalDate changeDate = ph.changedAt().toLocalDate();
            if (changeDate.isBefore(rangeStart) || changeDate.isAfter(rangeEnd)) {
                continue;
            }

            LocalDate beforeStart = changeDate.minusDays(WINDOW_DAYS);
            LocalDate beforeEnd = changeDate.minusDays(1);
            LocalDate afterStart = changeDate;
            LocalDate afterEnd = changeDate.plusDays(WINDOW_DAYS - 1L);

            BigDecimal avgBefore = avgDailyInWindow(dailySales, beforeStart, beforeEnd);
            BigDecimal avgAfter = avgDailyInWindow(dailySales, afterStart, afterEnd);

            BigDecimal priceChangePct = pctChange(ph.oldPrice(), ph.newPrice());
            BigDecimal qtyChangePct = pctChange(avgBefore, avgAfter);

            String summary = buildSummary(ph, avgBefore, avgAfter, qtyChangePct);

            out.add(DssPriceChangeImpactResponse.builder()
                .changedAt(ph.changedAt())
                .oldPrice(ph.oldPrice())
                .newPrice(ph.newPrice())
                .priceChangePercent(priceChangePct)
                .avgDailyQtyBefore(avgBefore)
                .avgDailyQtyAfter(avgAfter)
                .quantityChangePercent(qtyChangePct)
                .windowDays(WINDOW_DAYS)
                .summary(summary)
                .build());
        }
        return out;
    }

    private static BigDecimal avgDailyInWindow(
        Map<LocalDate, Long> dailySales,
        LocalDate start,
        LocalDate end
    ) {
        if (start.isAfter(end)) {
            return BigDecimal.ZERO;
        }
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        if (days <= 0) {
            return BigDecimal.ZERO;
        }
        long sum = 0;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            sum += dailySales.getOrDefault(d, 0L);
        }
        return BigDecimal.valueOf(sum)
            .divide(BigDecimal.valueOf(days), SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal pctChange(BigDecimal before, BigDecimal after) {
        if (before == null || after == null || before.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return after.subtract(before)
            .divide(before, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100))
            .setScale(1, RoundingMode.HALF_UP);
    }

    private static String buildSummary(
        PriceHistoryInfo ph,
        BigDecimal avgBefore,
        BigDecimal avgAfter,
        BigDecimal qtyChangePct
    ) {
        OffsetDateTime at = ph.changedAt();
        String priceDir = ph.newPrice().compareTo(ph.oldPrice()) > 0 ? "tăng" : "giảm";
        if (avgBefore.compareTo(BigDecimal.ZERO) == 0 && avgAfter.compareTo(BigDecimal.ZERO) == 0) {
            return String.format(
                "Ngày %s: %s giá — chưa đủ đơn trong ±%d ngày để đánh giá.",
                at.toLocalDate(),
                priceDir,
                WINDOW_DAYS
            );
        }
        String qtyDir = qtyChangePct.compareTo(BigDecimal.ZERO) > 0
            ? "tăng"
            : qtyChangePct.compareTo(BigDecimal.ZERO) < 0 ? "giảm" : "ổn định";
        return String.format(
            "Ngày %s: %s giá từ %s → %s VND; TB bán/ngày %s → %s SP (%s %s%% trong ±%d ngày).",
            at.toLocalDate(),
            priceDir,
            ph.oldPrice().stripTrailingZeros().toPlainString(),
            ph.newPrice().stripTrailingZeros().toPlainString(),
            avgBefore,
            avgAfter,
            qtyDir,
            qtyChangePct.abs(),
            WINDOW_DAYS
        );
    }
}

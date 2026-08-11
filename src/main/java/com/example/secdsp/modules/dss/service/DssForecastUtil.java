package com.example.secdsp.modules.dss.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Dự báo nhu cầu: trung bình có trọng số (ưu tiên ngày gần) + hệ số xu hướng.
 */
public final class DssForecastUtil {

    private static final int SCALE = 4;

    private DssForecastUtil() {
    }

    public record ForecastResult(
        BigDecimal averageDailyDemand,
        BigDecimal trendFactor,
        BigDecimal predictedQuantity,
        String methodology,
        LocalDate historicalFrom,
        LocalDate historicalTo,
        int forecastDays
    ) {}

    /**
     * @param dailyQty map date → quantity sold (chỉ ngày có bán)
     */
    public static ForecastResult forecast(
        Map<LocalDate, Long> dailyQty,
        LocalDate startDate,
        LocalDate endDate,
        int forecastDays
    ) {
        long totalDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (totalDays <= 0 || forecastDays <= 0) {
            return emptyResult(startDate, endDate, forecastDays);
        }

        long totalQty = dailyQty.values().stream().mapToLong(Long::longValue).sum();
        if (totalQty <= 0) {
            return emptyResult(startDate, endDate, forecastDays);
        }

        // Trung bình có trọng số: ngày gần hiện tại trọng số cao hơn
        BigDecimal weightedSum = BigDecimal.ZERO;
        BigDecimal weightTotal = BigDecimal.ZERO;
        List<LocalDate> sortedDates = dailyQty.keySet().stream().sorted().toList();

        for (LocalDate d : sortedDates) {
            long daysFromStart = java.time.temporal.ChronoUnit.DAYS.between(startDate, d) + 1;
            BigDecimal weight = BigDecimal.valueOf(daysFromStart);
            BigDecimal qty = BigDecimal.valueOf(dailyQty.getOrDefault(d, 0L));
            weightedSum = weightedSum.add(qty.multiply(weight));
            weightTotal = weightTotal.add(weight);
        }

        BigDecimal weightedAvgDaily = weightTotal.compareTo(BigDecimal.ZERO) > 0
            ? weightedSum.divide(weightTotal, SCALE, RoundingMode.HALF_UP)
            : BigDecimal.valueOf(totalQty)
                .divide(BigDecimal.valueOf(totalDays), SCALE, RoundingMode.HALF_UP);

        // Xu hướng: so sánh nửa đầu vs nửa sau của kỳ lịch sử
        BigDecimal trendFactor = computeTrendFactor(dailyQty, startDate, endDate);

        BigDecimal adjustedDaily = weightedAvgDaily
            .multiply(BigDecimal.ONE.add(trendFactor.multiply(new BigDecimal("0.5"))))
            .max(BigDecimal.ZERO);

        BigDecimal predicted = adjustedDaily
            .multiply(BigDecimal.valueOf(forecastDays))
            .setScale(2, RoundingMode.HALF_UP);

        String methodology = String.format(
            "Trung bình có trọng số %d ngày lịch sử (%s → %s), "
                + "điều chỉnh xu hướng %s%%, dự báo %d ngày tới.",
            totalDays,
            startDate,
            endDate,
            trendFactor.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP),
            forecastDays
        );

        return new ForecastResult(
            weightedAvgDaily.setScale(2, RoundingMode.HALF_UP),
            trendFactor.setScale(4, RoundingMode.HALF_UP),
            predicted,
            methodology,
            startDate,
            endDate,
            forecastDays
        );
    }

    /** Fallback SMA khi không có daily breakdown. */
    public static ForecastResult simpleAverage(
        long totalQuantity,
        int historicalDays,
        int forecastDays,
        LocalDate startDate,
        LocalDate endDate
    ) {
        BigDecimal avg = BigDecimal.valueOf(totalQuantity)
            .divide(BigDecimal.valueOf(Math.max(historicalDays, 1)), 2, RoundingMode.HALF_UP);
        BigDecimal predicted = avg.multiply(BigDecimal.valueOf(forecastDays))
            .setScale(2, RoundingMode.HALF_UP);
        String methodology = String.format(
            "Trung bình đơn giản: %d SP / %d ngày → dự báo %d ngày tới.",
            totalQuantity,
            historicalDays,
            forecastDays
        );
        return new ForecastResult(
            avg,
            BigDecimal.ZERO,
            predicted,
            methodology,
            startDate,
            endDate,
            forecastDays
        );
    }

    private static BigDecimal computeTrendFactor(
        Map<LocalDate, Long> dailyQty,
        LocalDate startDate,
        LocalDate endDate
    ) {
        long span = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (span < 4) {
            return BigDecimal.ZERO;
        }
        LocalDate mid = startDate.plusDays(span / 2);

        double firstHalf = dailyQty.entrySet().stream()
            .filter(e -> !e.getKey().isAfter(mid))
            .mapToLong(Map.Entry::getValue)
            .sum();
        double secondHalf = dailyQty.entrySet().stream()
            .filter(e -> e.getKey().isAfter(mid))
            .mapToLong(Map.Entry::getValue)
            .sum();

        long firstDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, mid) + 1;
        long secondDays = java.time.temporal.ChronoUnit.DAYS.between(mid.plusDays(1), endDate) + 1;

        if (firstDays <= 0 || secondDays <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal avgFirst = BigDecimal.valueOf(firstHalf)
            .divide(BigDecimal.valueOf(firstDays), SCALE, RoundingMode.HALF_UP);
        BigDecimal avgSecond = BigDecimal.valueOf(secondHalf)
            .divide(BigDecimal.valueOf(secondDays), SCALE, RoundingMode.HALF_UP);

        if (avgFirst.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        return avgSecond.subtract(avgFirst)
            .divide(avgFirst, SCALE, RoundingMode.HALF_UP);
    }

    private static ForecastResult emptyResult(
        LocalDate startDate,
        LocalDate endDate,
        int forecastDays
    ) {
        return new ForecastResult(
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            "Không đủ dữ liệu bán hàng để dự báo.",
            startDate,
            endDate,
            forecastDays
        );
    }
}

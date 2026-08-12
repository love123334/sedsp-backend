package com.example.secdsp.modules.dss.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Dự báo nhu cầu: trung bình có trọng số + xu hướng + mùa vụ (thứ / ngày lễ).
 */
public final class DssForecastUtil {

    private static final int SCALE = 4;

    private DssForecastUtil() {
    }

    public record ForecastDayPoint(
        LocalDate date,
        BigDecimal predictedQty,
        String note
    ) {}

    public record ForecastResult(
        BigDecimal averageDailyDemand,
        BigDecimal trendFactor,
        BigDecimal predictedQuantity,
        BigDecimal seasonalityAdjustedQuantity,
        BigDecimal holidayAdjustmentFactor,
        String methodology,
        LocalDate historicalFrom,
        LocalDate historicalTo,
        int forecastDays,
        List<ForecastDayPoint> forecastSeries,
        List<DssHolidayCalendar.HolidayWindow> upcomingHolidays
    ) {}

    public static ForecastResult forecast(
        Map<LocalDate, Long> dailyQty,
        LocalDate startDate,
        LocalDate endDate,
        int forecastDays
    ) {
        long totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (totalDays <= 0 || forecastDays <= 0) {
            return emptyResult(startDate, endDate, forecastDays);
        }

        long totalQty = dailyQty.values().stream().mapToLong(Long::longValue).sum();
        if (totalQty <= 0) {
            return emptyResult(startDate, endDate, forecastDays);
        }

        BigDecimal weightedSum = BigDecimal.ZERO;
        BigDecimal weightTotal = BigDecimal.ZERO;
        List<LocalDate> sortedDates = dailyQty.keySet().stream().sorted().toList();

        for (LocalDate d : sortedDates) {
            long daysFromStart = ChronoUnit.DAYS.between(startDate, d) + 1;
            BigDecimal weight = BigDecimal.valueOf(daysFromStart);
            BigDecimal qty = BigDecimal.valueOf(dailyQty.getOrDefault(d, 0L));
            weightedSum = weightedSum.add(qty.multiply(weight));
            weightTotal = weightTotal.add(weight);
        }

        BigDecimal weightedAvgDaily = weightTotal.compareTo(BigDecimal.ZERO) > 0
            ? weightedSum.divide(weightTotal, SCALE, RoundingMode.HALF_UP)
            : BigDecimal.valueOf(totalQty)
                .divide(BigDecimal.valueOf(totalDays), SCALE, RoundingMode.HALF_UP);

        BigDecimal trendFactor = computeTrendFactor(dailyQty, startDate, endDate);

        BigDecimal adjustedDaily = weightedAvgDaily
            .multiply(BigDecimal.ONE.add(trendFactor.multiply(new BigDecimal("0.5"))))
            .max(BigDecimal.ZERO);

        BigDecimal flatPredicted = adjustedDaily
            .multiply(BigDecimal.valueOf(forecastDays))
            .setScale(2, RoundingMode.HALF_UP);

        Map<DayOfWeek, BigDecimal> dowFactors = DssSeasonalityUtil.dayOfWeekFactors(dailyQty);
        LocalDate forecastStart = endDate.plusDays(1);
        LocalDate forecastEnd = endDate.plusDays(forecastDays);
        List<DssHolidayCalendar.HolidayWindow> holidays =
            DssHolidayCalendar.holidaysBetween(forecastStart, forecastEnd);

        List<ForecastDayPoint> series = new ArrayList<>();
        BigDecimal seasonalitySum = BigDecimal.ZERO;
        for (int i = 0; i < forecastDays; i++) {
            LocalDate d = forecastStart.plusDays(i);
            BigDecimal dayQty = DssSeasonalityUtil.dailyForecast(adjustedDaily, d, dowFactors);
            seasonalitySum = seasonalitySum.add(dayQty);
            DssHolidayCalendar.HolidayWindow hw = DssHolidayCalendar.holidayOn(d);
            String note = hw == null ? null : hw.label();
            series.add(new ForecastDayPoint(d, dayQty, note));
        }

        BigDecimal seasonalityAdjusted = seasonalitySum.setScale(2, RoundingMode.HALF_UP);
        BigDecimal holidayFactor = flatPredicted.compareTo(BigDecimal.ZERO) > 0
            ? seasonalityAdjusted.divide(flatPredicted, SCALE, RoundingMode.HALF_UP)
            : BigDecimal.ONE;

        String methodology = String.format(
            "Trung bình có trọng số %d ngày (%s → %s), xu hướng %s%%, "
                + "điều chỉnh thứ trong tuần + %d sự kiện lịch trong kỳ dự báo.",
            totalDays,
            startDate,
            endDate,
            trendFactor.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP),
            holidays.size()
        );

        return new ForecastResult(
            weightedAvgDaily.setScale(2, RoundingMode.HALF_UP),
            trendFactor.setScale(4, RoundingMode.HALF_UP),
            flatPredicted,
            seasonalityAdjusted,
            holidayFactor.setScale(4, RoundingMode.HALF_UP),
            methodology,
            startDate,
            endDate,
            forecastDays,
            series,
            holidays
        );
    }

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

        LocalDate forecastStart = endDate.plusDays(1);
        List<DssHolidayCalendar.HolidayWindow> holidays =
            DssHolidayCalendar.holidaysBetween(forecastStart, endDate.plusDays(forecastDays));

        Map<DayOfWeek, BigDecimal> uniform = DssSeasonalityUtil.dayOfWeekFactors(Map.of());
        List<ForecastDayPoint> series = new ArrayList<>();
        BigDecimal seasonalitySum = BigDecimal.ZERO;
        for (int i = 0; i < forecastDays; i++) {
            LocalDate d = forecastStart.plusDays(i);
            BigDecimal dayQty = DssSeasonalityUtil.dailyForecast(avg, d, uniform);
            seasonalitySum = seasonalitySum.add(dayQty);
            DssHolidayCalendar.HolidayWindow hw = DssHolidayCalendar.holidayOn(d);
            series.add(new ForecastDayPoint(d, dayQty, hw == null ? null : hw.label()));
        }
        BigDecimal seasonalityAdjusted = seasonalitySum.setScale(2, RoundingMode.HALF_UP);
        BigDecimal holidayFactor = predicted.compareTo(BigDecimal.ZERO) > 0
            ? seasonalityAdjusted.divide(predicted, SCALE, RoundingMode.HALF_UP)
            : BigDecimal.ONE;

        String methodology = String.format(
            "Trung bình đơn giản: %d SP / %d ngày; bổ sung hệ số ngày lễ (%d sự kiện).",
            totalQuantity,
            historicalDays,
            holidays.size()
        );
        return new ForecastResult(
            avg,
            BigDecimal.ZERO,
            predicted,
            seasonalityAdjusted,
            holidayFactor,
            methodology,
            startDate,
            endDate,
            forecastDays,
            series,
            holidays
        );
    }

    private static BigDecimal computeTrendFactor(
        Map<LocalDate, Long> dailyQty,
        LocalDate startDate,
        LocalDate endDate
    ) {
        long span = ChronoUnit.DAYS.between(startDate, endDate) + 1;
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

        long firstDays = ChronoUnit.DAYS.between(startDate, mid) + 1;
        long secondDays = ChronoUnit.DAYS.between(mid.plusDays(1), endDate) + 1;

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
            BigDecimal.ZERO,
            BigDecimal.ONE,
            "Không đủ dữ liệu bán hàng để dự báo.",
            startDate,
            endDate,
            forecastDays,
            List.of(),
            List.of()
        );
    }
}

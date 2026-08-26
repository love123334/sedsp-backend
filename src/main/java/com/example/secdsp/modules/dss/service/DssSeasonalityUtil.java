package com.example.secdsp.modules.dss.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;

/**
 * Mùa vụ theo thứ trong tuần — điều chỉnh dự báo từng ngày tương lai.
 */
public final class DssSeasonalityUtil {

    private static final int SCALE = 4;

    private DssSeasonalityUtil() {
    }

    /** Hệ số theo thứ (1.0 = trung bình). Cần ≥14 ngày có bán để ước lượng. */
    public static Map<DayOfWeek, BigDecimal> dayOfWeekFactors(Map<LocalDate, Long> dailyQty) {
        Map<DayOfWeek, BigDecimal> sums = new EnumMap<>(DayOfWeek.class);
        Map<DayOfWeek, Integer> counts = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek d : DayOfWeek.values()) {
            sums.put(d, BigDecimal.ZERO);
            counts.put(d, 0);
        }

        for (Map.Entry<LocalDate, Long> e : dailyQty.entrySet()) {
            DayOfWeek dow = e.getKey().getDayOfWeek();
            sums.put(dow, sums.get(dow).add(BigDecimal.valueOf(e.getValue())));
            counts.put(dow, counts.get(dow) + 1);
        }

        BigDecimal overall = BigDecimal.ZERO;
        int totalCount = 0;
        for (DayOfWeek d : DayOfWeek.values()) {
            overall = overall.add(sums.get(d));
            totalCount += counts.get(d);
        }
        if (totalCount < 14 || overall.compareTo(BigDecimal.ZERO) <= 0) {
            return uniformFactors();
        }

        BigDecimal overallAvg = overall.divide(
            BigDecimal.valueOf(totalCount),
            SCALE,
            RoundingMode.HALF_UP
        );

        Map<DayOfWeek, BigDecimal> factors = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek d : DayOfWeek.values()) {
            int c = counts.get(d);
            if (c == 0) {
                factors.put(d, BigDecimal.ONE);
                continue;
            }
            BigDecimal dowAvg = sums.get(d)
                .divide(BigDecimal.valueOf(c), SCALE, RoundingMode.HALF_UP);
            BigDecimal factor = dowAvg.divide(overallAvg, SCALE, RoundingMode.HALF_UP);
            factors.put(d, clamp(factor, new BigDecimal("0.65"), new BigDecimal("1.45")));
        }
        return factors;
    }

    /** Nhu cầu 1 ngày tương lai = baseDaily × dowFactor. */
    public static BigDecimal dailyForecast(
        BigDecimal baseDaily,
        LocalDate date,
        Map<DayOfWeek, BigDecimal> dowFactors
    ) {
        BigDecimal dow = dowFactors.getOrDefault(date.getDayOfWeek(), BigDecimal.ONE);
        return baseDaily.multiply(dow)
            .max(BigDecimal.ZERO)
            .setScale(2, RoundingMode.HALF_UP);
    }

    private static Map<DayOfWeek, BigDecimal> uniformFactors() {
        Map<DayOfWeek, BigDecimal> f = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek d : DayOfWeek.values()) {
            f.put(d, BigDecimal.ONE);
        }
        return f;
    }

    private static BigDecimal clamp(BigDecimal v, BigDecimal min, BigDecimal max) {
        if (v.compareTo(min) < 0) return min;
        if (v.compareTo(max) > 0) return max;
        return v;
    }
}

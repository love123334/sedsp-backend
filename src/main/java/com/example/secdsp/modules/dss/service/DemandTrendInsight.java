package com.example.secdsp.modules.dss.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Compares OLS trend on history vs the forecast horizon and explains
 * divergence only when the two directions differ.
 */
public final class DemandTrendInsight {

    public static final double SLOPE_THRESHOLD = 0.02;
    private static final DateTimeFormatter VI_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public enum Direction {
        UP,
        DOWN,
        STABLE
    }

    private DemandTrendInsight() {
    }

    public static Direction classify(double slope) {
        if (slope >= SLOPE_THRESHOLD) {
            return Direction.UP;
        }
        if (slope <= -SLOPE_THRESHOLD) {
            return Direction.DOWN;
        }
        return Direction.STABLE;
    }

    public static String labelVi(Direction direction) {
        return switch (direction) {
            case UP -> "Đang tăng";
            case DOWN -> "Đang giảm";
            case STABLE -> "Tương đối ổn định";
        };
    }

    public static String code(Direction direction) {
        return switch (direction) {
            case UP -> "up";
            case DOWN -> "down";
            case STABLE -> "stable";
        };
    }

    public static Map<String, Object> analyze(
        LocalDate historyStart,
        List<Long> history,
        List<Double> forecast,
        double historySlope,
        double seasonalityStrength
    ) {
        Direction historyDir = classify(historySlope);
        double forecastSlope = linearRegressionSlope(forecast);
        Direction forecastDir = classify(forecastSlope);

        int n = history.size();
        int recentWindow = recentWindowSize(n);
        double recentSlope = n >= 4
            ? linearRegressionSlope(tail(history, recentWindow))
            : historySlope;
        double earlierSlope = n > recentWindow + 3
            ? linearRegressionSlope(head(history, n - recentWindow))
            : historySlope;
        Direction recentDir = classify(recentSlope);
        Direction earlierDir = classify(earlierSlope);

        LocalDate trendBreakDate = null;
        if (n >= 8 && recentDir != earlierDir && recentDir != Direction.STABLE) {
            trendBreakDate = historyStart.plusDays(n - recentWindow);
        }

        String reason = null;
        if (historyDir != forecastDir) {
            reason = divergenceReason(
                historyStart,
                history,
                forecast,
                historyDir,
                forecastDir,
                recentDir,
                earlierDir,
                recentWindow,
                trendBreakDate,
                seasonalityStrength
            );
        }

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("historyTrend", code(historyDir));
        snapshot.put("historyTrendLabel", labelVi(historyDir));
        snapshot.put("forecastTrendDirection", code(forecastDir));
        snapshot.put("forecastTrendLabel", labelVi(forecastDir));
        snapshot.put("forecastTrendSlope", round4(forecastSlope));
        snapshot.put("trendBreakDate", trendBreakDate == null ? null : trendBreakDate.toString());
        snapshot.put("trendDivergenceReason", reason);
        return snapshot;
    }

    static String divergenceReason(
        LocalDate historyStart,
        List<Long> history,
        List<Double> forecast,
        Direction historyDir,
        Direction forecastDir,
        Direction recentDir,
        Direction earlierDir,
        int recentWindow,
        LocalDate trendBreakDate,
        double seasonalityStrength
    ) {
        if (trendBreakDate != null && recentDir == forecastDir && recentDir != historyDir) {
            return "Đột phá xu hướng từ ngày " + trendBreakDate.format(VI_DATE)
                + ": " + recentWindow + " ngày gần nhất "
                + labelVi(recentDir).toLowerCase()
                + ", trong khi cả cửa sổ lịch sử vẫn "
                + labelVi(historyDir).toLowerCase() + ".";
        }

        double recentAvg = average(tail(history, recentWindow));
        double earlierAvg = history.size() > recentWindow
            ? average(head(history, history.size() - recentWindow))
            : recentAvg;

        if (forecastDir == Direction.DOWN && recentAvg < earlierAvg * 0.85) {
            return "Suy giảm nhu cầu: trung bình " + recentWindow
                + " ngày gần nhất thấp hơn giai đoạn trước, nên dự báo "
                + labelVi(forecastDir).toLowerCase()
                + " dù lịch sử " + labelVi(historyDir).toLowerCase() + ".";
        }

        if (forecastDir == Direction.UP && recentAvg > earlierAvg * 1.15) {
            return "Nhu cầu tăng tốc gần đây so với phần đầu cửa sổ lịch sử, nên dự báo "
                + labelVi(forecastDir).toLowerCase()
                + " dù lịch sử " + labelVi(historyDir).toLowerCase() + ".";
        }

        if (seasonalityStrength >= 0.08 && !forecast.isEmpty()) {
            double firstHalf = averageDoubles(forecast.subList(0, Math.max(1, (forecast.size() + 1) / 2)));
            double secondHalf = averageDoubles(
                forecast.subList(Math.max(1, (forecast.size() + 1) / 2), forecast.size())
            );
            String windowLabel;
            if (secondHalf > firstHalf + 0.25) {
                windowLabel = "nửa sau kỳ rơi vào cuối tuần (T6–CN)";
            } else if (firstHalf > secondHalf + 0.25) {
                windowLabel = "cuối tuần đi qua sớm, nửa sau là ngày thường";
            } else {
                windowLabel = weekendHeavy(historyStart.plusDays(history.size()), forecast.size())
                    ? "nhịp cuối tuần (T6–CN)"
                    : "nhịp ngày thường trong tuần";
            }
            return "Kỳ dự báo " + labelVi(forecastDir).toLowerCase()
                + " vì " + windowLabel
                + ", khác với xu hướng lịch sử "
                + labelVi(historyDir).toLowerCase() + ".";
        }

        return "Đường dự báo " + labelVi(forecastDir).toLowerCase()
            + " trong khi lịch sử " + labelVi(historyDir).toLowerCase()
            + " vì mô hình ưu tiên tín hiệu gần đây và mùa theo thứ.";
    }

    static int recentWindowSize(int historyDays) {
        if (historyDays < 8) {
            return Math.max(2, historyDays / 2);
        }
        return Math.min(7, Math.max(5, historyDays / 4));
    }

    static boolean weekendHeavy(LocalDate forecastStart, int horizon) {
        int weekendDays = 0;
        LocalDate date = forecastStart;
        for (int i = 0; i < horizon; i++) {
            int iso = date.getDayOfWeek().getValue();
            if (iso >= 5) {
                weekendDays++;
            }
            date = date.plusDays(1);
        }
        return weekendDays * 2 >= horizon;
    }

    static double linearRegressionSlope(List<? extends Number> series) {
        if (series == null || series.size() < 2) {
            return 0.0;
        }
        int n = series.size();
        double sumX = 0.0;
        double sumY = 0.0;
        double sumXY = 0.0;
        double sumX2 = 0.0;
        for (int i = 0; i < n; i++) {
            double x = i + 1.0;
            double y = series.get(i).doubleValue();
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }
        double denominator = (n * sumX2) - (sumX * sumX);
        if (denominator == 0.0) {
            return 0.0;
        }
        return ((n * sumXY) - (sumX * sumY)) / denominator;
    }

    private static List<Long> tail(List<Long> series, int window) {
        int start = Math.max(0, series.size() - window);
        return new ArrayList<>(series.subList(start, series.size()));
    }

    private static List<Long> head(List<Long> series, int length) {
        int end = Math.max(0, Math.min(length, series.size()));
        return new ArrayList<>(series.subList(0, end));
    }

    private static double averageDoubles(List<Double> series) {
        if (series == null || series.isEmpty()) {
            return 0.0;
        }
        double sum = 0.0;
        for (double value : series) {
            sum += value;
        }
        return sum / series.size();
    }

    private static double average(List<Long> series) {
        if (series.isEmpty()) {
            return 0.0;
        }
        long sum = 0L;
        for (long value : series) {
            sum += value;
        }
        return sum / (double) series.size();
    }

    private static double round4(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }
}

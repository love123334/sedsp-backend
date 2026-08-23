package com.example.secdsp.modules.dss.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Explainable demand forecasting: Moving Average → Holt linear → Holt-Winters
 * based on history length and seasonality strength.
 */
public final class StatisticalForecastEngine {

    public static final String METHOD_MOVING_AVERAGE = "moving_average";
    public static final String METHOD_HOLT_LINEAR = "holt_linear";
    public static final String METHOD_HOLT_WINTERS = "holt_winters";

    private static final int WEEKLY_PERIOD = 7;
    private static final double SEASONALITY_THRESHOLD = 0.18;

    private StatisticalForecastEngine() {
    }

    public enum Strategy {
        MOVING_AVERAGE(METHOD_MOVING_AVERAGE),
        HOLT_LINEAR(METHOD_HOLT_LINEAR),
        HOLT_WINTERS(METHOD_HOLT_WINTERS);

        private final String methodId;

        Strategy(String methodId) {
            this.methodId = methodId;
        }

        public String methodId() {
            return methodId;
        }
    }

    public record ForecastResult(
        Strategy strategy,
        String methodId,
        double level,
        double trend,
        double seasonalityStrength,
        List<Double> dailyForecast
    ) {
    }

    /**
     * &lt; 14 days → MA; 14–60 → Holt; &gt; 60 + strong weekly seasonality → Holt-Winters.
     */
    public static Strategy selectStrategy(
        int historyDays,
        long positiveDays,
        double seasonalityStrength
    ) {
        if (historyDays < 14 || positiveDays < 5) {
            return Strategy.MOVING_AVERAGE;
        }
        if (historyDays >= 60 && seasonalityStrength >= SEASONALITY_THRESHOLD) {
            return Strategy.HOLT_WINTERS;
        }
        return Strategy.HOLT_LINEAR;
    }

    /** Weekly seasonality strength in [0, 1] from day-of-week means vs overall mean. */
    public static double seasonalityStrength(
        LocalDate startDate,
        List<Long> dailySeries
    ) {
        if (dailySeries.size() < 14) {
            return 0.0;
        }

        double[] dowSum = new double[7];
        int[] dowCount = new int[7];
        double total = 0.0;
        int positive = 0;

        LocalDate date = startDate;
        for (long qty : dailySeries) {
            int dow = date.getDayOfWeek().getValue() % 7;
            dowSum[dow] += qty;
            dowCount[dow]++;
            if (qty > 0) {
                total += qty;
                positive++;
            }
            date = date.plusDays(1);
        }

        if (positive < 7) {
            return 0.0;
        }

        double overallMean = total / positive;
        if (overallMean <= 0.0) {
            return 0.0;
        }

        double var = 0.0;
        int buckets = 0;
        for (int i = 0; i < 7; i++) {
            if (dowCount[i] == 0) {
                continue;
            }
            double mean = dowSum[i] / dowCount[i];
            var += Math.abs(mean - overallMean);
            buckets++;
        }

        if (buckets == 0) {
            return 0.0;
        }

        return Math.min(1.0, (var / buckets) / overallMean);
    }

    public static ForecastResult forecast(
        LocalDate startDate,
        List<Long> dailySeries,
        int horizon,
        Strategy strategy
    ) {
        List<Double> predictions = switch (strategy) {
            case MOVING_AVERAGE -> movingAverageForecast(dailySeries, horizon);
            case HOLT_LINEAR -> holtLinearForecast(dailySeries, horizon);
            case HOLT_WINTERS -> holtWintersForecast(startDate, dailySeries, horizon);
        };

        double level = predictions.isEmpty() ? 0.0 : predictions.get(0);
        double trend = dailySeries.size() >= 2
            ? linearRegressionSlope(dailySeries)
            : 0.0;

        return new ForecastResult(
            strategy,
            strategy.methodId(),
            level,
            trend,
            seasonalityStrength(startDate, dailySeries),
            predictions
        );
    }

    private static List<Double> movingAverageForecast(List<Long> series, int horizon) {
        int window = Math.min(7, series.size());
        double avg = averageOfTail(series, window);
        List<Double> out = new ArrayList<>(horizon);
        for (int i = 0; i < horizon; i++) {
            out.add(Math.max(0.0, avg));
        }
        return out;
    }

    private static List<Double> holtLinearForecast(List<Long> series, int horizon) {
        double[] y = toArray(series);
        if (y.length == 0) {
            return zeroHorizon(horizon);
        }

        double alpha = 0.35;
        double beta = 0.12;
        double level = y[0];
        double trend = y.length > 1 ? y[1] - y[0] : 0.0;

        for (int t = 1; t < y.length; t++) {
            double prevLevel = level;
            level = alpha * y[t] + (1.0 - alpha) * (level + trend);
            trend = beta * (level - prevLevel) + (1.0 - beta) * trend;
        }

        List<Double> out = new ArrayList<>(horizon);
        for (int h = 1; h <= horizon; h++) {
            out.add(Math.max(0.0, level + h * trend));
        }
        return out;
    }

    private static List<Double> holtWintersForecast(
        LocalDate startDate,
        List<Long> series,
        int horizon
    ) {
        double[] y = toArray(series);
        int n = y.length;
        int m = WEEKLY_PERIOD;

        if (n < m * 2) {
            return holtLinearForecast(series, horizon);
        }

        double alpha = 0.25;
        double beta = 0.08;
        double gamma = 0.15;

        double[] seasonal = new double[m];
        Arrays.fill(seasonal, 0.0);
        for (int i = 0; i < m; i++) {
            seasonal[i] = y[i] - average(y, 0, m);
        }

        double level = average(y, 0, m);
        double trend = (average(y, m, Math.min(2 * m, n)) - average(y, 0, m)) / m;

        for (int t = 0; t < n; t++) {
            int si = t % m;
            double value = y[t];
            double prevLevel = level;
            double prevSeason = seasonal[si];

            level = alpha * (value - prevSeason) + (1.0 - alpha) * (level + trend);
            trend = beta * (level - prevLevel) + (1.0 - beta) * trend;
            seasonal[si] = gamma * (value - level) + (1.0 - gamma) * prevSeason;
        }

        List<Double> out = new ArrayList<>(horizon);
        for (int h = 1; h <= horizon; h++) {
            int si = (n + h - 1) % m;
            out.add(Math.max(0.0, level + h * trend + seasonal[si]));
        }
        return out;
    }

    private static double[] toArray(List<Long> series) {
        return series.stream().mapToDouble(Long::doubleValue).toArray();
    }

    private static List<Double> zeroHorizon(int horizon) {
        List<Double> out = new ArrayList<>(horizon);
        for (int i = 0; i < horizon; i++) {
            out.add(0.0);
        }
        return out;
    }

    private static double average(double[] values, int from, int to) {
        if (to <= from) {
            return 0.0;
        }
        double sum = 0.0;
        for (int i = from; i < to && i < values.length; i++) {
            sum += values[i];
        }
        return sum / (to - from);
    }

    private static double averageOfTail(List<Long> series, int window) {
        if (series.isEmpty() || window <= 0) {
            return 0.0;
        }
        int start = Math.max(0, series.size() - window);
        long sum = 0L;
        for (int i = start; i < series.size(); i++) {
            sum += series.get(i);
        }
        return sum / (double) (series.size() - start);
    }

    private static double linearRegressionSlope(List<Long> series) {
        if (series.size() < 2) {
            return 0.0;
        }
        int n = series.size();
        double sumX = 0.0;
        double sumY = 0.0;
        double sumXY = 0.0;
        double sumX2 = 0.0;
        for (int i = 0; i < n; i++) {
            double x = i + 1.0;
            double y = series.get(i);
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
}

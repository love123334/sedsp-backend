package com.example.secdsp.modules.dss.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Advanced explainable demand forecasting:
 * - Moving Average (Short/Sparse series)
 * - Croston / SBA (Intermittent / Sparse demand)
 * - Damped Holt Linear (Trend with damping factor phi = 0.90)
 * - Damped Holt-Winters (Trend + Seasonality with adaptive alpha/beta/gamma grid optimization)
 */
public final class StatisticalForecastEngine {

    public static final String METHOD_MOVING_AVERAGE = "moving_average";
    public static final String METHOD_HOLT_LINEAR = "holt_linear";
    public static final String METHOD_HOLT_WINTERS = "holt_winters";
    public static final String METHOD_CROSTON_SBA = "croston_sba";

    private static final int WEEKLY_PERIOD = 7;
    private static final double SEASONALITY_THRESHOLD = 0.08;
    private static final double DEFAULT_DAMPING_PHI = 0.95;
    private static final double FLAT_FORECAST_RANGE = 0.25;
    private static final double SLOPE_PERSISTENCE = 0.85;

    private StatisticalForecastEngine() {
    }

    public enum Strategy {
        MOVING_AVERAGE(METHOD_MOVING_AVERAGE),
        HOLT_LINEAR(METHOD_HOLT_LINEAR),
        HOLT_WINTERS(METHOD_HOLT_WINTERS),
        CROSTON_SBA(METHOD_CROSTON_SBA);

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
     * Strategy Selection:
     * - &lt; 14 days or &lt; 4 positive days -> MA
     * - >= 14 days and positive ratio &lt; 35% -> Croston SBA (Intermittent)
     * - >= 14 days + seasonal strength >= 0.08 + >= 6 positive days -> Holt-Winters
     * - Otherwise -> Holt Linear
     */
    public static Strategy selectStrategy(
        int historyDays,
        long positiveDays,
        double seasonalityStrength
    ) {
        if (historyDays < 14 || positiveDays < 4) {
            return Strategy.MOVING_AVERAGE;
        }
        double positiveRatio = (double) positiveDays / Math.max(1, historyDays);
        if (historyDays >= 14 && positiveRatio < 0.35 && positiveDays >= 3) {
            return Strategy.CROSTON_SBA;
        }
        if (historyDays >= 14 && seasonalityStrength >= SEASONALITY_THRESHOLD && positiveDays >= 6) {
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

        if (positive < 6) {
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
        // Step 1: Outlier winsorization (smooth single-day extreme shocks > 2.5 sigma)
        double[] smoothed = winsorizeSeries(dailySeries);

        List<Double> predictions = switch (strategy) {
            case MOVING_AVERAGE -> movingAverageForecast(smoothed, horizon);
            case CROSTON_SBA -> crostonSbaForecast(smoothed, horizon);
            case HOLT_LINEAR -> adaptiveHoltLinearForecast(smoothed, horizon);
            case HOLT_WINTERS -> adaptiveHoltWintersForecast(startDate, smoothed, horizon);
        };

        if (strategy != Strategy.CROSTON_SBA) {
            predictions = rescueDegenerateForecast(
                startDate,
                dailySeries,
                predictions,
                horizon
            );
        }

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

    /** Outlier smoothing to prevent level distortion from temporary 1-day spikes */
    private static double[] winsorizeSeries(List<Long> series) {
        double[] y = toArray(series);
        if (y.length < 7) {
            return y;
        }
        double sum = 0.0;
        for (double v : y) sum += v;
        double mean = sum / y.length;

        double variance = 0.0;
        for (double v : y) variance += (v - mean) * (v - mean);
        double std = Math.sqrt(variance / y.length);
        double threshold = mean + 2.5 * Math.max(1.0, std);

        double[] out = new double[y.length];
        for (int i = 0; i < y.length; i++) {
            out[i] = Math.min(y[i], threshold);
        }
        return out;
    }

    private static List<Double> movingAverageForecast(double[] y, int horizon) {
        int window = Math.min(7, y.length);
        double sum = 0.0;
        int count = 0;
        for (int i = Math.max(0, y.length - window); i < y.length; i++) {
            sum += y[i];
            count++;
        }
        double avg = count > 0 ? sum / count : 0.0;
        List<Double> out = new ArrayList<>(horizon);
        for (int i = 0; i < horizon; i++) {
            out.add(Math.max(0.0, avg));
        }
        return out;
    }

    /** Croston's Syntetos-Boylan Approximation (SBA) for intermittent demand */
    private static List<Double> crostonSbaForecast(double[] y, int horizon) {
        if (y.length == 0) return zeroHorizon(horizon);

        double alpha = 0.15;
        double z = 0.0; // Demand magnitude
        double p = 1.0; // Demand interval
        int periodsSinceDemand = 1;
        boolean firstOccurrence = true;

        for (double val : y) {
            if (val > 0) {
                if (firstOccurrence) {
                    z = val;
                    p = Math.max(1.0, periodsSinceDemand);
                    firstOccurrence = false;
                } else {
                    z = alpha * val + (1.0 - alpha) * z;
                    p = alpha * periodsSinceDemand + (1.0 - alpha) * p;
                }
                periodsSinceDemand = 1;
            } else {
                periodsSinceDemand++;
            }
        }

        if (p <= 0.0) p = 1.0;
        // Syntetos-Boylan bias correction
        double sbaEstimate = Math.max(0.0, (1.0 - (alpha / 2.0)) * (z / p));

        List<Double> out = new ArrayList<>(horizon);
        for (int i = 0; i < horizon; i++) {
            out.add(sbaEstimate);
        }
        return out;
    }

    /** Damped Holt Linear with adaptive grid search optimization (MSE minimization) */
    private static List<Double> adaptiveHoltLinearForecast(double[] y, int horizon) {
        int n = y.length;
        if (n == 0) return zeroHorizon(horizon);
        if (n < 3) return movingAverageForecast(y, horizon);

        double phi = DEFAULT_DAMPING_PHI;
        double bestMse = Double.MAX_VALUE;
        double bestAlpha = 0.30;
        double bestBeta = 0.08;

        // Grid search for optimal (alpha, beta) on historical one-step-ahead MSE
        double[] alphaCandidates = {0.15, 0.30, 0.45};
        double[] betaCandidates = {0.03, 0.08, 0.15, 0.25};

        for (double a : alphaCandidates) {
            for (double b : betaCandidates) {
                double mse = evaluateHoltLinearMse(y, a, b, phi);
                if (mse < bestMse) {
                    bestMse = mse;
                    bestAlpha = a;
                    bestBeta = b;
                }
            }
        }

        double level = y[0];
        double trend = y.length > 1 ? y[1] - y[0] : 0.0;

        for (int t = 1; t < n; t++) {
            double prevLevel = level;
            level = bestAlpha * y[t] + (1.0 - bestAlpha) * (level + phi * trend);
            trend = bestBeta * (level - prevLevel) + (1.0 - bestBeta) * phi * trend;
        }

        List<Double> out = new ArrayList<>(horizon);
        double cumulativeDamp = 0.0;
        for (int h = 1; h <= horizon; h++) {
            cumulativeDamp += Math.pow(phi, h);
            out.add(Math.max(0.0, level + cumulativeDamp * trend));
        }
        return out;
    }

    private static double evaluateHoltLinearMse(double[] y, double alpha, double beta, double phi) {
        int n = y.length;
        double level = y[0];
        double trend = y.length > 1 ? y[1] - y[0] : 0.0;
        double sumSqErr = 0.0;

        for (int t = 1; t < n; t++) {
            double forecast1Step = level + phi * trend;
            double error = y[t] - forecast1Step;
            sumSqErr += error * error;

            double prevLevel = level;
            level = alpha * y[t] + (1.0 - alpha) * (level + phi * trend);
            trend = beta * (level - prevLevel) + (1.0 - beta) * phi * trend;
        }
        return sumSqErr / Math.max(1, n - 1);
    }

    /** Damped Holt-Winters with weekly seasonality and adaptive parameter optimization */
    private static List<Double> adaptiveHoltWintersForecast(
        LocalDate startDate,
        double[] y,
        int horizon
    ) {
        int n = y.length;
        int m = WEEKLY_PERIOD;

        if (n < m * 2) {
            return adaptiveHoltLinearForecast(y, horizon);
        }

        double phi = DEFAULT_DAMPING_PHI;
        double bestMse = Double.MAX_VALUE;
        double bestAlpha = 0.25;
        double bestBeta = 0.06;
        double bestGamma = 0.15;

        double[] alphaCandidates = {0.15, 0.25, 0.35};
        double[] betaCandidates = {0.03, 0.06, 0.10};
        double[] gammaCandidates = {0.10, 0.15, 0.25};

        for (double a : alphaCandidates) {
            for (double b : betaCandidates) {
                for (double g : gammaCandidates) {
                    double mse = evaluateHoltWintersMse(y, a, b, g, phi, m);
                    if (mse < bestMse) {
                        bestMse = mse;
                        bestAlpha = a;
                        bestBeta = b;
                        bestGamma = g;
                    }
                }
            }
        }

        double[] seasonal = new double[m];
        double baseAvg = average(y, 0, m);
        for (int i = 0; i < m; i++) {
            seasonal[i] = y[i] - baseAvg;
        }

        double level = baseAvg;
        double trend = (average(y, m, Math.min(2 * m, n)) - average(y, 0, m)) / m;

        for (int t = 0; t < n; t++) {
            int si = t % m;
            double value = y[t];
            double prevLevel = level;
            double prevSeason = seasonal[si];

            level = bestAlpha * (value - prevSeason) + (1.0 - bestAlpha) * (level + phi * trend);
            trend = bestBeta * (level - prevLevel) + (1.0 - bestBeta) * phi * trend;
            seasonal[si] = bestGamma * (value - level) + (1.0 - bestGamma) * prevSeason;
        }

        List<Double> out = new ArrayList<>(horizon);
        double cumulativeDamp = 0.0;
        for (int h = 1; h <= horizon; h++) {
            int si = (n + h - 1) % m;
            cumulativeDamp += Math.pow(phi, h);
            out.add(Math.max(0.0, level + cumulativeDamp * trend + seasonal[si]));
        }
        return out;
    }

    private static double evaluateHoltWintersMse(
        double[] y,
        double alpha,
        double beta,
        double gamma,
        double phi,
        int m
    ) {
        int n = y.length;
        double[] seasonal = new double[m];
        double baseAvg = average(y, 0, m);
        for (int i = 0; i < m; i++) {
            seasonal[i] = y[i] - baseAvg;
        }

        double level = baseAvg;
        double trend = (average(y, m, Math.min(2 * m, n)) - average(y, 0, m)) / m;
        double sumSqErr = 0.0;
        int count = 0;

        for (int t = 0; t < n; t++) {
            int si = t % m;
            if (t >= m) {
                double forecast1Step = level + phi * trend + seasonal[si];
                double error = y[t] - forecast1Step;
                sumSqErr += error * error;
                count++;
            }

            double value = y[t];
            double prevLevel = level;
            double prevSeason = seasonal[si];

            level = alpha * (value - prevSeason) + (1.0 - alpha) * (level + phi * trend);
            trend = beta * (level - prevLevel) + (1.0 - beta) * phi * trend;
            seasonal[si] = gamma * (value - level) + (1.0 - gamma) * prevSeason;
        }
        return count > 0 ? sumSqErr / count : 0.0;
    }

    /**
     * Holt / MA often collapse noisy 2–5 unit series to a constant mean.
     * If the horizon is visually flat, re-project with the OLS slope and
     * weekly day-of-week means so the forecast line actually moves.
     */
    private static List<Double> rescueDegenerateForecast(
        LocalDate startDate,
        List<Long> dailySeries,
        List<Double> predictions,
        int horizon
    ) {
        if (predictions.isEmpty() || horizon < 2 || dailySeries.size() < 2) {
            return predictions;
        }

        double predMin = Double.POSITIVE_INFINITY;
        double predMax = Double.NEGATIVE_INFINITY;
        for (double value : predictions) {
            predMin = Math.min(predMin, value);
            predMax = Math.max(predMax, value);
        }
        double predRange = predMax - predMin;
        double slope = linearRegressionSlope(dailySeries);
        double seasonal = seasonalityStrength(startDate, dailySeries);
        boolean needTrend = predRange < FLAT_FORECAST_RANGE && Math.abs(slope) >= 0.01;
        boolean needSeason = dailySeries.size() >= 14 && seasonal >= 0.06;

        if (!needTrend && !needSeason) {
            return predictions;
        }

        double level = averageOfTail(dailySeries, Math.min(7, dailySeries.size()));
        if (level <= 0.0 && !predictions.isEmpty()) {
            level = predictions.get(0);
        }

        double[] dowMean = dayOfWeekMeans(startDate, dailySeries);
        double overall = seriesMean(dailySeries);

        List<Double> out = new ArrayList<>(horizon);
        LocalDate next = startDate.plusDays(dailySeries.size());
        for (int h = 1; h <= horizon; h++) {
            double base = needTrend
                ? level + slope * h * SLOPE_PERSISTENCE
                : predictions.get(h - 1);
            double seas = 0.0;
            if (needSeason) {
                int dow = next.getDayOfWeek().getValue() % 7;
                seas = dowMean[dow] - overall;
            }
            out.add(Math.max(0.0, base + seas));
            next = next.plusDays(1);
        }
        return out;
    }

    private static double[] dayOfWeekMeans(LocalDate startDate, List<Long> dailySeries) {
        double[] sum = new double[7];
        int[] count = new int[7];
        LocalDate date = startDate;
        for (long qty : dailySeries) {
            int dow = date.getDayOfWeek().getValue() % 7;
            sum[dow] += qty;
            count[dow]++;
            date = date.plusDays(1);
        }
        double filled = 0.0;
        int buckets = 0;
        double[] mean = new double[7];
        for (int i = 0; i < 7; i++) {
            if (count[i] > 0) {
                mean[i] = sum[i] / count[i];
                filled += mean[i];
                buckets++;
            }
        }
        double fallback = buckets > 0 ? filled / buckets : 0.0;
        for (int i = 0; i < 7; i++) {
            if (count[i] == 0) {
                mean[i] = fallback;
            }
        }
        return mean;
    }

    private static double seriesMean(List<Long> series) {
        if (series.isEmpty()) {
            return 0.0;
        }
        long sum = 0L;
        for (long value : series) {
            sum += value;
        }
        return sum / (double) series.size();
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

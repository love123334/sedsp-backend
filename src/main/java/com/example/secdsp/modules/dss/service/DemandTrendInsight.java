package com.example.secdsp.modules.dss.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Interpretation layer on top of the forecast series.
 * Historical direction and forecast movement are classified separately,
 * then combined into a seller-facing story. A tiny negative slope on a
 * high plateau is "ổn định ở mức cao", not "đang giảm".
 */
public final class DemandTrendInsight {

    public static final double SLOPE_THRESHOLD = 0.02;
    static final double SEASONAL_LABEL_THRESHOLD = 0.12;
    private static final double STRONG_LIFT = 1.50;
    private static final double HIGH_LEVEL_LIFT = 1.35;
    /** Intra-horizon % change below this is a plateau, not a new trend. */
    private static final double SIDEWAYS_PCT = 0.18;
    /** Share of day-to-day steps that must agree before we call a short walk a trend. */
    private static final double WALK_AGREEMENT = 0.65;
    private static final DateTimeFormatter VI_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public enum Direction {
        UP,
        DOWN,
        STABLE,
        SEASONAL
    }

    public enum Combined {
        CONTINUE_UP,
        UP_TO_HIGH_STABLE,
        UP_TO_STABLE,
        UP_THEN_COOL,
        STABLE,
        SEASONAL,
        MAY_RISE,
        MAY_FALL,
        CONTINUE_DOWN,
        DOWN_TO_STABLE,
        RECOVERING
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
            case SEASONAL -> "Theo mùa tuần";
        };
    }

    public static String code(Direction direction) {
        return switch (direction) {
            case UP -> "up";
            case DOWN -> "down";
            case STABLE -> "stable";
            case SEASONAL -> "seasonal";
        };
    }

    public static Map<String, Object> analyze(
        LocalDate historyStart,
        List<Long> history,
        List<Double> forecast,
        double historySlope,
        double seasonalityStrength
    ) {
        int n = history.size();
        int levelWindow = levelWindowSize(n);
        double recentLevel = average(tail(history, levelWindow));
        double earlierLevel = average(head(history, Math.max(levelWindow, n / 2)));
        double liftRatio = recentLevel / Math.max(1.0, earlierLevel);
        boolean highLevel = recentLevel >= earlierLevel * HIGH_LEVEL_LIFT && recentLevel >= 1.5;
        boolean lowLevel = recentLevel <= earlierLevel * 0.75 && earlierLevel >= 1.5;

        Direction historyBase = classify(historySlope);
        if (liftRatio >= STRONG_LIFT) {
            historyBase = Direction.UP;
        } else if (liftRatio <= 0.67 && earlierLevel >= 1.5) {
            historyBase = Direction.DOWN;
        }
        Direction historyDir = withSeasonalLabel(historyBase, seasonalityStrength);
        String historyLabel = historyLabelVi(historyDir, liftRatio);

        Direction forecastDir = classifyForecastMovement(
            forecast,
            recentLevel,
            seasonalityStrength
        );
        String forecastLabel = forecastMovementLabel(forecastDir, highLevel, lowLevel);

        Combined combined = combine(historyDir, forecastDir, highLevel);
        double forecastSlope = levelSlope(forecast);

        int recentWindow = recentWindowSize(n);
        double recentSlope = n >= 4
            ? linearRegressionSlope(tail(history, recentWindow))
            : historySlope;
        double earlierSlope = n > recentWindow + 3
            ? linearRegressionSlope(head(history, n - recentWindow))
            : historySlope;
        Direction recentDir = classify(recentSlope);
        Direction earlierDir = classify(earlierSlope);

        boolean weeklyCycleNotBreak = seasonalityStrength >= SEASONAL_LABEL_THRESHOLD
            && Math.abs(meanWeekdayResidual(historyStart, history, recentWindow))
                < 0.25 * Math.max(1.0, average(history));
        if (weeklyCycleNotBreak) {
            recentDir = stripSeasonal(historyDir);
        }

        LocalDate trendBreakDate = null;
        if (!weeklyCycleNotBreak
            && n >= 8
            && recentDir != earlierDir
            && recentDir != Direction.STABLE) {
            trendBreakDate = historyStart.plusDays(n - recentWindow);
        }

        String reason = null;
        if (combined == Combined.UP_THEN_COOL
            || combined == Combined.RECOVERING
            || combined == Combined.MAY_FALL
            || combined == Combined.MAY_RISE) {
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
        snapshot.put("historyTrendLabel", historyLabel);
        snapshot.put("forecastTrendDirection", code(forecastDir));
        snapshot.put("forecastTrendLabel", forecastLabel);
        snapshot.put("forecastTrendSlope", round4(forecastSlope));
        snapshot.put("trendBreakDate", trendBreakDate == null ? null : trendBreakDate.toString());
        snapshot.put("trendDivergenceReason", reason);
        snapshot.put("trendCombined", combinedCode(combined));
        snapshot.put("trendInsightLabel", combinedLabel(combined));
        snapshot.put("trendInsightDetail", insightDetail(combined));
        snapshot.put("trendRecommendation", recommendation(combined));
        return snapshot;
    }

    static Direction classifyForecastMovement(
        List<Double> forecast,
        double recentHistMean,
        double seasonalityStrength
    ) {
        if (forecast == null || forecast.size() < 2) {
            return withSeasonalLabel(Direction.STABLE, seasonalityStrength);
        }
        double forecastMean = averageNumbers(forecast);
        int mid = Math.max(1, (forecast.size() + 1) / 2);
        double first = forecast.size() >= 14
            ? averageNumbers(forecast.subList(0, 7))
            : averageNumbers(forecast.subList(0, mid));
        double last = forecast.size() >= 14
            ? averageNumbers(forecast.subList(forecast.size() - 7, forecast.size()))
            : averageNumbers(forecast.subList(mid, forecast.size()));
        double intraPct = (last - first) / Math.max(1.0, first);
        double vsRecent = (forecastMean - recentHistMean) / Math.max(1.0, recentHistMean);

        if (intraPct <= -SIDEWAYS_PCT) {
            return Direction.DOWN;
        }
        if (intraPct >= SIDEWAYS_PCT) {
            return Direction.UP;
        }

        Direction walk = consistentWalk(forecast, forecastMean);
        if (walk != Direction.STABLE) {
            return walk;
        }

        if (Math.abs(vsRecent) <= SIDEWAYS_PCT) {
            return withSeasonalLabel(Direction.STABLE, seasonalityStrength);
        }
        if (vsRecent > SIDEWAYS_PCT) {
            return Direction.UP;
        }
        if (vsRecent < -SIDEWAYS_PCT) {
            return Direction.DOWN;
        }
        return withSeasonalLabel(Direction.STABLE, seasonalityStrength);
    }

    /**
     * Tiny OLS slopes like -0.06/day on a level of ~12 are noise.
     * A 7-day run of 14.0, 14.4, … 16.4 is a real walk up.
     */
    static Direction consistentWalk(List<Double> forecast, double mean) {
        if (forecast == null || forecast.size() < 3) {
            return Direction.STABLE;
        }
        double eps = Math.max(0.05, 0.02 * Math.abs(mean));
        int up = 0;
        int down = 0;
        int steps = forecast.size() - 1;
        for (int i = 1; i < forecast.size(); i++) {
            double delta = forecast.get(i) - forecast.get(i - 1);
            if (delta > eps) {
                up++;
            } else if (delta < -eps) {
                down++;
            }
        }
        double need = WALK_AGREEMENT * steps;
        if (up >= need) {
            return Direction.UP;
        }
        if (down >= need) {
            return Direction.DOWN;
        }
        return Direction.STABLE;
    }

    static Combined combine(Direction historyDir, Direction forecastDir, boolean highLevel) {
        Direction history = stripSeasonal(historyDir);
        Direction forecast = stripSeasonal(forecastDir);
        if (historyDir == Direction.SEASONAL
            && (forecastDir == Direction.SEASONAL || forecast == Direction.STABLE)) {
            return Combined.SEASONAL;
        }
        if (history == Direction.UP && forecast == Direction.UP) {
            return Combined.CONTINUE_UP;
        }
        if (history == Direction.UP && forecast == Direction.STABLE) {
            return highLevel ? Combined.UP_TO_HIGH_STABLE : Combined.UP_TO_STABLE;
        }
        if (history == Direction.UP && forecast == Direction.DOWN) {
            return Combined.UP_THEN_COOL;
        }
        if (history == Direction.STABLE && forecast == Direction.UP) {
            return Combined.MAY_RISE;
        }
        if (history == Direction.STABLE && forecast == Direction.DOWN) {
            return Combined.MAY_FALL;
        }
        if (history == Direction.DOWN && forecast == Direction.DOWN) {
            return Combined.CONTINUE_DOWN;
        }
        if (history == Direction.DOWN && forecast == Direction.STABLE) {
            return Combined.DOWN_TO_STABLE;
        }
        if (history == Direction.DOWN && forecast == Direction.UP) {
            return Combined.RECOVERING;
        }
        return historyDir == Direction.SEASONAL ? Combined.SEASONAL : Combined.STABLE;
    }

    static String combinedLabel(Combined combined) {
        return switch (combined) {
            case CONTINUE_UP -> "Tăng và tiếp tục tăng";
            case UP_TO_HIGH_STABLE -> "Tăng → ổn định ở mức cao";
            case UP_TO_STABLE -> "Tăng → ổn định";
            case UP_THEN_COOL -> "Tăng nhưng có dấu hiệu hạ nhiệt";
            case STABLE -> "Ổn định";
            case SEASONAL -> "Theo mùa tuần";
            case MAY_RISE -> "Có khả năng tăng";
            case MAY_FALL -> "Có khả năng giảm";
            case CONTINUE_DOWN -> "Giảm rõ rệt";
            case DOWN_TO_STABLE -> "Giảm → ổn định";
            case RECOVERING -> "Có dấu hiệu phục hồi";
        };
    }

    static String combinedCode(Combined combined) {
        return switch (combined) {
            case CONTINUE_UP -> "continue_up";
            case UP_TO_HIGH_STABLE -> "up_to_high_stable";
            case UP_TO_STABLE -> "up_to_stable";
            case UP_THEN_COOL -> "up_then_cool";
            case STABLE -> "stable";
            case SEASONAL -> "seasonal";
            case MAY_RISE -> "may_rise";
            case MAY_FALL -> "may_fall";
            case CONTINUE_DOWN -> "continue_down";
            case DOWN_TO_STABLE -> "down_to_stable";
            case RECOVERING -> "recovering";
        };
    }

    static String historyLabelVi(Direction direction, double liftRatio) {
        if (direction == Direction.UP && liftRatio >= STRONG_LIFT) {
            return "Đang tăng mạnh";
        }
        if (direction == Direction.DOWN && liftRatio <= 0.67) {
            return "Đang giảm mạnh";
        }
        return labelVi(direction);
    }

    static String forecastMovementLabel(Direction direction, boolean highLevel, boolean lowLevel) {
        if (direction == Direction.STABLE && highLevel) {
            return "Ổn định ở mức cao";
        }
        if (direction == Direction.STABLE && lowLevel) {
            return "Ổn định ở mức thấp";
        }
        if (direction == Direction.STABLE) {
            return "Ổn định";
        }
        return labelVi(direction);
    }

    static String insightDetail(Combined combined) {
        return switch (combined) {
            case UP_TO_HIGH_STABLE, UP_TO_STABLE ->
                "Nhu cầu tăng rõ trong giai đoạn gần đây và dự báo duy trì ở mức cao, dù có dao động nhẹ.";
            case CONTINUE_UP ->
                "Nhu cầu vừa tăng và dự báo tiếp tục đi lên.";
            case UP_THEN_COOL ->
                "Nhu cầu gần đây đã tăng, nhưng đường dự báo đang hạ dần so với mức đó.";
            case SEASONAL ->
                "Nhu cầu lặp nhịp theo tuần; dự báo giữ pattern đó chứ không đổi hướng dài hạn.";
            case RECOVERING ->
                "Lịch sử đang giảm nhưng dự báo đảo chiều tăng — tín hiệu phục hồi.";
            case CONTINUE_DOWN ->
                "Nhu cầu đang giảm và dự báo tiếp tục yếu.";
            case DOWN_TO_STABLE ->
                "Nhu cầu đã giảm rồi dự báo giữ ổn định ở mức mới.";
            case MAY_RISE ->
                "Lịch sử tương đối ổn định, dự báo nghiêng tăng.";
            case MAY_FALL ->
                "Lịch sử tương đối ổn định, nhưng kỳ dự báo nghiêng giảm.";
            case STABLE ->
                "Nhu cầu và dự báo đều đi ngang, không đổi hướng dài hạn.";
        };
    }

    static String recommendation(Combined combined) {
        return switch (combined) {
            case UP_TO_HIGH_STABLE, UP_TO_STABLE ->
                "Nhu cầu gần đây đang tăng mạnh và dự báo duy trì ở mức cao. Nên giữ tồn kho cao hơn giai đoạn trước và theo dõi xem mức này có đứng vững sau 2–4 tuần.";
            case CONTINUE_UP ->
                "Dự báo tiếp tục tăng — chủ động nhập thêm, tránh hết hàng giữa kỳ.";
            case UP_THEN_COOL ->
                "Đã tăng nhưng có dấu hiệu hạ nhiệt. Giữ tồn đủ cho nhịp hiện tại, chưa tăng nhập mạnh thêm.";
            case SEASONAL ->
                "Chuẩn bị tồn theo nhịp tuần (cuối tuần thường cao hơn).";
            case RECOVERING ->
                "Có tín hiệu phục hồi. Tăng tồn nhẹ theo dự báo và theo dõi 1–2 tuần.";
            case CONTINUE_DOWN ->
                "Nhu cầu giảm rõ — hạ tồn, tránh nhập dày.";
            case DOWN_TO_STABLE ->
                "Đã giảm rồi đi ngang. Điều chỉnh tồn về mức mới, chưa cắt sâu thêm.";
            case MAY_RISE ->
                "Có khả năng tăng. Sẵn sàng tồn đệm vừa phải.";
            case MAY_FALL ->
                "Có khả năng giảm. Giữ tồn vừa, ưu tiên xả chậm nếu bán chậm hơn kỳ trước.";
            case STABLE ->
                "Nhu cầu ổn định. Duy trì tồn xoay vòng, tránh nhập đột biến.";
        };
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
                + " ngày gần nhất thấp hơn giai đoạn trước.";
        }

        if (forecastDir == Direction.UP && recentAvg > earlierAvg * 1.15) {
            return "Nhu cầu tăng tốc gần đây so với phần đầu cửa sổ lịch sử.";
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
                + " vì " + windowLabel + ".";
        }

        return null;
    }

    static Direction withSeasonalLabel(Direction direction, double seasonalityStrength) {
        return direction == Direction.SEASONAL ? Direction.STABLE : direction;
    }

    static Direction stripSeasonal(Direction direction) {
        return direction == Direction.SEASONAL ? Direction.STABLE : direction;
    }

    static double levelSlope(List<? extends Number> series) {
        if (series == null || series.size() < 2) {
            return 0.0;
        }
        if (series.size() < 14) {
            int mid = Math.max(1, (series.size() + 1) / 2);
            double first = averageNumbers(series.subList(0, mid));
            double second = averageNumbers(series.subList(mid, series.size()));
            return (second - first) / Math.max(1, series.size() - mid);
        }
        double first = averageNumbers(series.subList(0, 7));
        double last = averageNumbers(series.subList(series.size() - 7, series.size()));
        return (last - first) / (series.size() - 7);
    }

    static double meanWeekdayResidual(
        LocalDate historyStart,
        List<Long> history,
        int recentWindow
    ) {
        if (history.isEmpty() || recentWindow <= 0) {
            return 0.0;
        }
        double[] dowSum = new double[7];
        int[] dowCount = new int[7];
        LocalDate date = historyStart;
        for (long qty : history) {
            int dow = date.getDayOfWeek().getValue() % 7;
            dowSum[dow] += qty;
            dowCount[dow]++;
            date = date.plusDays(1);
        }
        double[] dowMean = new double[7];
        for (int i = 0; i < 7; i++) {
            dowMean[i] = dowCount[i] > 0 ? dowSum[i] / dowCount[i] : 0.0;
        }

        int from = Math.max(0, history.size() - recentWindow);
        date = historyStart.plusDays(from);
        double residual = 0.0;
        int count = 0;
        for (int i = from; i < history.size(); i++) {
            int dow = date.getDayOfWeek().getValue() % 7;
            residual += history.get(i) - dowMean[dow];
            count++;
            date = date.plusDays(1);
        }
        return count > 0 ? residual / count : 0.0;
    }

    static int recentWindowSize(int historyDays) {
        if (historyDays < 8) {
            return Math.max(2, historyDays / 2);
        }
        return Math.min(7, Math.max(5, historyDays / 4));
    }

    static int levelWindowSize(int historyDays) {
        if (historyDays < 14) {
            return Math.max(3, historyDays / 2);
        }
        return Math.min(14, Math.max(7, historyDays / 6));
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

    private static double averageNumbers(List<? extends Number> series) {
        if (series == null || series.isEmpty()) {
            return 0.0;
        }
        double sum = 0.0;
        for (Number value : series) {
            sum += value.doubleValue();
        }
        return sum / series.size();
    }

    private static double averageDoubles(List<Double> series) {
        return averageNumbers(series);
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

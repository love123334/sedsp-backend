package com.example.secdsp.modules.dss.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemandTrendInsightTest {

    @Test
    void matchingDirectionsOmitAReason() {
        List<Long> history = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            history.add(4L + i / 3);
        }
        List<Double> forecast = List.of(14.0, 14.4, 14.8, 15.2, 15.6, 16.0, 16.4);
        double historySlope = DemandTrendInsight.linearRegressionSlope(history);

        Map<String, Object> insight = DemandTrendInsight.analyze(
            LocalDate.of(2026, 7, 28),
            history,
            forecast,
            historySlope,
            0.04
        );

        assertEquals("up", insight.get("historyTrend"));
        assertTrue(String.valueOf(insight.get("historyTrendLabel")).contains("tăng"));
        assertEquals("up", insight.get("forecastTrendDirection"));
        assertEquals("continue_up", insight.get("trendCombined"));
        assertNull(insight.get("trendDivergenceReason"));
    }

    @Test
    void recentReboundExplainsForecastBreakFromHistory() {
        List<Long> history = new ArrayList<>();
        for (int i = 0; i < 23; i++) {
            history.add(12L - i / 3);
        }
        history.addAll(List.of(3L, 4L, 5L, 6L, 7L, 8L, 9L));
        List<Double> forecast = List.of(9.2, 9.8, 10.4, 11.0, 11.6, 12.2, 12.8);
        double historySlope = DemandTrendInsight.linearRegressionSlope(history);

        Map<String, Object> insight = DemandTrendInsight.analyze(
            LocalDate.of(2026, 7, 28),
            history,
            forecast,
            historySlope,
            0.05
        );

        assertEquals("down", insight.get("historyTrend"));
        assertEquals("up", insight.get("forecastTrendDirection"));
        String reason = (String) insight.get("trendDivergenceReason");
        assertTrue(reason != null && reason.contains("Đột phá xu hướng"), reason);
        assertEquals("2026-08-20", insight.get("trendBreakDate"));
    }

    @Test
    void seasonalWindowExplainsStableHistoryVersusMovingForecast() {
        List<Long> history = new ArrayList<>();
        for (int i = 0; i < 28; i++) {
            history.add(5L);
        }
        List<Double> forecast = List.of(8.0, 10.0, 8.0, 3.0, 4.0, 4.0, 5.0);
        double historySlope = DemandTrendInsight.linearRegressionSlope(history);

        Map<String, Object> insight = DemandTrendInsight.analyze(
            LocalDate.of(2026, 7, 27),
            history,
            forecast,
            historySlope,
            0.35
        );

        assertEquals("seasonal", insight.get("historyTrend"));
        assertEquals("down", insight.get("forecastTrendDirection"));
        String reason = (String) insight.get("trendDivergenceReason");
        assertTrue(reason != null && reason.contains("cuối tuần đi qua sớm"), reason);
    }

    @Test
    void thirtyDayWeeklyForecastIsLabeledSeasonalNotDown() {
        LocalDate start = LocalDate.of(2026, 3, 1);
        int[] weekly = {3, 4, 4, 5, 8, 10, 8};
        List<Long> history = new ArrayList<>();
        for (int i = 0; i < 180; i++) {
            history.add((long) weekly[i % 7]);
        }
        List<Double> forecast = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            forecast.add((double) weekly[(180 + i) % 7]);
        }
        double historySlope = DemandTrendInsight.linearRegressionSlope(history);
        double seasonality = StatisticalForecastEngine.seasonalityStrength(start, history);

        Map<String, Object> insight = DemandTrendInsight.analyze(
            start,
            history,
            forecast,
            historySlope,
            seasonality
        );

        assertEquals("seasonal", insight.get("historyTrend"));
        assertEquals("Theo mùa tuần", insight.get("historyTrendLabel"));
        assertEquals("seasonal", insight.get("forecastTrendDirection"));
        assertEquals("Theo mùa tuần", insight.get("forecastTrendLabel"));
        assertNull(insight.get("trendDivergenceReason"));
        assertNull(insight.get("trendBreakDate"));
    }

    @Test
    void regimeShiftThenHighPlateauIsNotLabeledDecreasing() {
        List<Long> history = new ArrayList<>();
        for (int i = 0; i < 140; i++) {
            history.add(3L + (i % 3 == 0 ? 1L : 0L));
        }
        for (int i = 0; i < 40; i++) {
            history.add(4L + Math.round(i * 9.0 / 39.0));
        }
        List<Double> forecast = new ArrayList<>();
        double[] wiggle = {13.2, 14.0, 13.1, 12.4, 11.8, 12.2, 11.6};
        for (int i = 0; i < 30; i++) {
            forecast.add(wiggle[i % wiggle.length]);
        }
        double historySlope = DemandTrendInsight.linearRegressionSlope(history);

        Map<String, Object> insight = DemandTrendInsight.analyze(
            LocalDate.of(2026, 2, 28),
            history,
            forecast,
            historySlope,
            0.05
        );

        assertEquals("Đang tăng mạnh", insight.get("historyTrendLabel"));
        assertEquals("stable", insight.get("forecastTrendDirection"));
        assertEquals("Ổn định ở mức cao", insight.get("forecastTrendLabel"));
        assertEquals("up_to_high_stable", insight.get("trendCombined"));
        assertEquals("Tăng → ổn định ở mức cao", insight.get("trendInsightLabel"));
        assertNull(insight.get("trendDivergenceReason"));
        String rec = String.valueOf(insight.get("trendRecommendation"));
        assertTrue(rec.contains("mức cao"), rec);
        assertTrue(!rec.toLowerCase().contains("đang giảm"), rec);
    }

    @Test
    void tinyNegativeOlsSlopeOnHighPlateauIsNotDecreasing() {
        List<Long> history = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            history.add(3L + Math.round(i * 10.0 / 29.0));
        }
        List<Double> forecast = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            double noise = i % 3 == 0 ? 0.4 : -0.2;
            forecast.add(13.5 - 0.06 * i + noise);
        }
        double historySlope = DemandTrendInsight.linearRegressionSlope(history);
        double forecastOls = DemandTrendInsight.linearRegressionSlope(forecast);

        Map<String, Object> insight = DemandTrendInsight.analyze(
            LocalDate.of(2026, 7, 27),
            history,
            forecast,
            historySlope,
            0.04
        );

        assertTrue(forecastOls < -0.02, "old slope rule would have labeled this down: " + forecastOls);
        assertEquals("Đang tăng mạnh", insight.get("historyTrendLabel"));
        assertEquals("stable", insight.get("forecastTrendDirection"));
        assertEquals("Ổn định ở mức cao", insight.get("forecastTrendLabel"));
        assertEquals("up_to_high_stable", insight.get("trendCombined"));
        assertEquals("Tăng → ổn định ở mức cao", insight.get("trendInsightLabel"));
        String rec = String.valueOf(insight.get("trendRecommendation"));
        assertTrue(!rec.toLowerCase().contains("đang giảm"), rec);
        assertTrue(!rec.toLowerCase().contains("hạ nhiệt"), rec);
    }

    @Test
    void genuineForecastCoolingAfterARampIsUpThenCool() {
        List<Long> history = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            history.add(3L + Math.round(i * 10.0 / 29.0));
        }
        List<Double> forecast = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            forecast.add(13.0 - i * 0.22);
        }

        Map<String, Object> insight = DemandTrendInsight.analyze(
            LocalDate.of(2026, 7, 27),
            history,
            forecast,
            DemandTrendInsight.linearRegressionSlope(history),
            0.04
        );

        assertEquals("down", insight.get("forecastTrendDirection"));
        assertEquals("up_then_cool", insight.get("trendCombined"));
        assertEquals("Tăng nhưng có dấu hiệu hạ nhiệt", insight.get("trendInsightLabel"));
    }
}

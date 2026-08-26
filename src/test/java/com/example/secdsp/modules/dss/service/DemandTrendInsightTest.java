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
        assertEquals("Đang tăng", insight.get("historyTrendLabel"));
        assertEquals("up", insight.get("forecastTrendDirection"));
        assertEquals("Đang tăng", insight.get("forecastTrendLabel"));
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

        assertEquals("stable", insight.get("historyTrend"));
        assertEquals("down", insight.get("forecastTrendDirection"));
        String reason = (String) insight.get("trendDivergenceReason");
        assertTrue(reason != null && reason.contains("cuối tuần đi qua sớm"), reason);
    }
}

package com.example.secdsp.modules.dss.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatisticalForecastEngineTest {

    @Test
    void selectStrategyUsesMovingAverageForShortHistory() {
        assertEquals(
            StatisticalForecastEngine.Strategy.MOVING_AVERAGE,
            StatisticalForecastEngine.selectStrategy(10, 8, 0.4)
        );
    }

    @Test
    void selectStrategyUsesHoltLinearForMidHistory() {
        assertEquals(
            StatisticalForecastEngine.Strategy.HOLT_LINEAR,
            StatisticalForecastEngine.selectStrategy(30, 20, 0.05)
        );
    }

    @Test
    void selectStrategyUsesHoltWintersForLongSeasonalHistory() {
        assertEquals(
            StatisticalForecastEngine.Strategy.HOLT_WINTERS,
            StatisticalForecastEngine.selectStrategy(
                90,
                70,
                0.25
            )
        );
    }

    @Test
    void holtLinearForecastsUpwardTrend() {
        LocalDate start = LocalDate.now().minusDays(29);
        List<Long> series = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            series.add((long) (3 + i / 3));
        }

        var result = StatisticalForecastEngine.forecast(
            start,
            series,
            7,
            StatisticalForecastEngine.Strategy.HOLT_LINEAR
        );

        assertEquals(7, result.dailyForecast().size());
        assertTrue(result.dailyForecast().get(6) >= result.dailyForecast().get(0));
        assertTrue(result.trend() > 0.0);
    }

    @Test
    void oscillatingSeriesDoesNotCollapseToAConstantMean() {
        LocalDate start = LocalDate.of(2026, 7, 28);
        int[] weekly = {3, 5, 4, 3, 2, 4, 3};
        List<Long> series = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            series.add((long) Math.max(1, weekly[i % 7] - (i >= 20 ? 1 : 0)));
        }

        var result = StatisticalForecastEngine.forecast(
            start,
            series,
            7,
            StatisticalForecastEngine.Strategy.HOLT_LINEAR
        );

        double min = result.dailyForecast().stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double max = result.dailyForecast().stream().mapToDouble(Double::doubleValue).max().orElse(0);
        assertTrue(
            max - min > 0.2,
            "forecast should move with trend/season, not stay a flat mean: " + result.dailyForecast()
        );
        assertTrue(
            Math.abs(result.dailyForecast().get(6) - result.dailyForecast().get(0)) > 0.05
                || max - min > 0.4,
            "first and last forecast days should not look identical: " + result.dailyForecast()
        );
    }
}

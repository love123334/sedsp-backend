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
            StatisticalForecastEngine.selectStrategy(30, 20, 0.1)
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
}

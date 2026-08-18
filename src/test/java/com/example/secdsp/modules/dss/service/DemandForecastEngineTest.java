package com.example.secdsp.modules.dss.service;

import com.example.secdsp.modules.dss.dto.internal.DemandForecastComputation;
import com.example.secdsp.modules.dss.dto.internal.DemandForecastProductView;
import com.example.secdsp.modules.inventory.entity.Inventory;
import com.example.secdsp.modules.inventory.repository.InventoryRepository;
import com.example.secdsp.modules.order.repository.OrderItemRepository;
import com.example.secdsp.modules.review.repository.ProductReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemandForecastEngineTest {

    private static final Long PRODUCT_ID = 15L;

    @Mock
    OrderItemRepository orderItemRepository;

    @Mock
    InventoryRepository inventoryRepository;

    @Mock
    ProductReviewRepository productReviewRepository;

    @Mock
    LightGbmOnnxDemandPredictor lightGbmPredictor;

    @InjectMocks
    DemandForecastEngine demandForecastEngine;

    @Test
    void forecastBuildsTrendAwareSeriesAndFeatureSnapshot() {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(13L);
        List<Object[]> rows = new ArrayList<>();
        for (int i = 0; i < 14; i++) {
            LocalDate date = startDate.plusDays(i);
            rows.add(new Object[] { Date.valueOf(date), (long) (i + 1) });
        }

        when(orderItemRepository.findCompletedDailySalesByProduct(
            anyLong(),
            any(LocalDateTime.class),
            any(LocalDateTime.class)
        )).thenReturn(rows);
        when(inventoryRepository.findByProduct_Id(PRODUCT_ID))
            .thenReturn(java.util.Optional.of(
                Inventory.builder()
                    .availableQuantity(42)
                    .reservedQuantity(3)
                    .build()
            ));
        when(productReviewRepository.getRatingSummary(PRODUCT_ID))
            .thenReturn(new Object[] { 4.6, 8L });

        DemandForecastComputation forecast = demandForecastEngine.forecast(
            new DemandForecastProductView(
                PRODUCT_ID,
                7L,
                "Nike Air Force",
                new BigDecimal("2500000.00")
            ),
            14,
            7
        );

        assertFalse(forecast.insufficientData());
        assertEquals(14, forecast.historicalDays());
        assertEquals(7, forecast.forecastDays());
        assertEquals(14, forecast.historicalSales().size());
        assertEquals(7, forecast.forecastSales().size());
        assertTrue(forecast.predictedDemand() > 0);
        assertTrue(
            ((Number) forecast.featureSnapshot().get("trendSlope")).doubleValue()
                > 0.0
        );
        assertEquals(42, forecast.featureSnapshot().get("currentStock"));
        assertEquals(8L, forecast.featureSnapshot().get("reviewCount"));
        assertEquals(
            "trend_blended_feature_forecast",
            forecast.method()
        );
    }

    @Test
    void forecastReturnsInsufficientDataWhenHistoryIsEmpty() {
        when(orderItemRepository.findCompletedDailySalesByProduct(
            anyLong(),
            any(LocalDateTime.class),
            any(LocalDateTime.class)
        )).thenReturn(List.of());
        when(inventoryRepository.findByProduct_Id(PRODUCT_ID))
            .thenReturn(java.util.Optional.empty());
        when(productReviewRepository.getRatingSummary(PRODUCT_ID))
            .thenReturn(new Object[] { null, 0L });

        DemandForecastComputation forecast = demandForecastEngine.forecast(
            new DemandForecastProductView(
                PRODUCT_ID,
                7L,
                "Nike Air Force",
                new BigDecimal("2500000.00")
            ),
            14,
            7
        );

        assertTrue(forecast.insufficientData());
        assertEquals(0L, forecast.predictedDemand());
        assertEquals(0.0, forecast.averageDailyDemand(), 0.0001);
        assertTrue(forecast.forecastSales().isEmpty());
        assertEquals(14, forecast.historicalSales().size());
    }

    @Test
    void forecastSupportsAnExplicitHistoricalDateRange() {
        LocalDate fromDate = LocalDate.now().minusDays(29);
        LocalDate toDate = LocalDate.now();
        when(orderItemRepository.findCompletedDailySalesByProduct(
            PRODUCT_ID,
            fromDate.atStartOfDay(),
            toDate.plusDays(1).atStartOfDay()
        )).thenReturn(List.of(
            new Object[] { Date.valueOf(fromDate), 4L },
            new Object[] { Date.valueOf(fromDate.plusDays(1)), 5L },
            new Object[] { Date.valueOf(fromDate.plusDays(2)), 6L }
        ));
        when(inventoryRepository.findByProduct_Id(PRODUCT_ID))
            .thenReturn(java.util.Optional.empty());
        when(productReviewRepository.getRatingSummary(PRODUCT_ID))
            .thenReturn(new Object[] { null, 0L });

        DemandForecastComputation forecast = demandForecastEngine.forecast(
            new DemandForecastProductView(
                PRODUCT_ID,
                7L,
                "Nike Air Force",
                new BigDecimal("2500000.00")
            ),
            fromDate,
            toDate,
            14
        );

        assertFalse(forecast.insufficientData());
        assertEquals(30, forecast.historicalDays());
        assertEquals(14, forecast.forecastDays());
        assertEquals(30, forecast.historicalSales().size());
        assertEquals(toDate.plusDays(1).toString(),
            forecast.forecastSales().get(0).get("date"));
    }
}

package com.example.secdsp.modules.dss.service;

import com.example.secdsp.common.exception.BusinessException;
import com.example.secdsp.modules.dss.dto.response.SellerDiscountAnalysisResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SellerDiscountProfitCalculatorTest {

    private final SellerDiscountProfitCalculator calculator =
        new SellerDiscountProfitCalculator();

    @Test
    void calculateReturnsExpectedDiscountSimulation() {
        SellerDiscountAnalysisResponse response = calculator.calculate(
            new BigDecimal("100"),
            new BigDecimal("70"),
            new BigDecimal("10"),
            100,
            -1.8
        );

        assertEquals(new BigDecimal("100.00"), response.getCurrentPrice());
        assertEquals(new BigDecimal("70.00"), response.getCostPrice());
        assertEquals(new BigDecimal("90.00"), response.getNewPrice());
        assertEquals(100L, response.getForecastDemand());
        assertEquals(118L, response.getPredictedDemand());
        assertEquals(new BigDecimal("3000.00"), response.getCurrentProfit());
        assertEquals(new BigDecimal("2360.00"), response.getExpectedProfit());
        assertEquals(150L, response.getBreakEvenQuantity());
        assertEquals(50L, response.getAdditionalUnitsRequired());
        assertEquals(
            "Mức giảm giá đề xuất có thể làm giảm lợi nhuận nếu không đạt thêm doanh số cần thiết.",
            response.getBusinessInsight()
        );
    }

    @Test
    void calculateReturnsMaintainInsightWithinThreePercent() {
        SellerDiscountAnalysisResponse response = calculator.calculate(
            new BigDecimal("100"),
            new BigDecimal("70"),
            new BigDecimal("1"),
            100,
            -1
        );

        assertEquals(
            "Chiến lược giảm giá có khả năng duy trì mức lợi nhuận hiện tại.",
            response.getBusinessInsight()
        );
    }

    @Test
    void calculateReturnsIncreaseInsightWhenExpectedProfitIsHigher() {
        SellerDiscountAnalysisResponse response = calculator.calculate(
            new BigDecimal("100"),
            new BigDecimal("20"),
            new BigDecimal("10"),
            100,
            -3
        );

        assertEquals(
            "Chiến lược giảm giá dự kiến sẽ làm tăng tổng lợi nhuận.",
            response.getBusinessInsight()
        );
    }

    @Test
    void calculateRejectsNewPriceAtOrBelowCost() {
        assertThrows(
            BusinessException.class,
            () -> calculator.calculate(
                new BigDecimal("100"),
                new BigDecimal("95"),
                new BigDecimal("10"),
                100,
                -1.8
            )
        );
    }
}

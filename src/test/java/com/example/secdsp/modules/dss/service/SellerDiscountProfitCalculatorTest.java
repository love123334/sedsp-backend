package com.example.secdsp.modules.dss.service;

import com.example.secdsp.common.exception.BusinessException;
import com.example.secdsp.config.DssProperties;
import com.example.secdsp.modules.dss.dto.response.SellerDiscountAnalysisResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SellerDiscountProfitCalculatorTest {

    private SellerDiscountProfitCalculator calculator;

    @BeforeEach
    void setUp() {
        DssProperties props = new DssProperties();
        props.setIncludeShippingInProfit(false);
        props.setIncludePlatformFee(false);
        props.setOperatingCostPerUnitVnd(BigDecimal.ZERO);
        props.setMaxPriceChangePercent(300);
        DssProfitCalculator profitCalculator = new DssProfitCalculator(props);
        DssScenarioEngine engine = new DssScenarioEngine(props, profitCalculator);
        calculator = new SellerDiscountProfitCalculator(props, profitCalculator, engine);
    }

    @Test
    void calculateReturnsExpectedDiscountSimulation() {
        SellerDiscountAnalysisResponse response = calculator.calculate(
            new BigDecimal("100"),
            new BigDecimal("70"),
            new BigDecimal("-10"),
            100,
            -1.8,
            30,
            "Test",
            "Test methodology"
        );

        assertEquals(new BigDecimal("100.00"), response.getCurrentPrice());
        assertEquals(new BigDecimal("90.00"), response.getNewPrice());
        assertEquals(118L, response.getPredictedDemand());
        assertEquals(new BigDecimal("2360.00"), response.getExpectedProfit());
    }

    @Test
    void calculateReturnsIncreaseForPositivePriceChange() {
        SellerDiscountAnalysisResponse response = calculator.calculate(
            new BigDecimal("100"),
            new BigDecimal("20"),
            new BigDecimal("10"),
            100,
            -1.5,
            30,
            "Test",
            "Test"
        );

        assertEquals(new BigDecimal("110.00"), response.getNewPrice());
        assertTrueLessDemand(response.getPredictedDemand(), 100L);
    }

    @Test
    void calculateRejectsNewPriceAtOrBelowCost() {
        assertThrows(
            BusinessException.class,
            () -> calculator.calculate(
                new BigDecimal("100"),
                new BigDecimal("95"),
                new BigDecimal("-10"),
                100,
                -1.8,
                30,
                "Test",
                "Test"
            )
        );
    }

    private void assertTrueLessDemand(long predicted, long base) {
        org.junit.jupiter.api.Assertions.assertTrue(predicted < base);
    }
}

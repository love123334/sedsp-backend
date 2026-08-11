package com.example.secdsp.modules.dss.service;

import com.example.secdsp.config.DssProperties;
import com.example.secdsp.modules.dss.dto.response.DssProfitBreakdownResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@RequiredArgsConstructor
public class DssScenarioEngine {

    private static final int CALCULATION_SCALE = 4;
    private static final int MONEY_SCALE = 2;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final DssProperties dssProperties;
    private final DssProfitCalculator profitCalculator;

    public record DemandEstimate(long quantity, BigDecimal demandChangeRate) {}

    public DemandEstimate estimateDemand(
        BigDecimal baseDailyDemand,
        int forecastDays,
        BigDecimal elasticity,
        int priceChangePercent
    ) {
        BigDecimal priceChangeRate = BigDecimal.valueOf(priceChangePercent)
            .divide(ONE_HUNDRED, CALCULATION_SCALE, RoundingMode.HALF_UP);
        BigDecimal demandChangeRate = elasticity.multiply(priceChangeRate);
        BigDecimal baseQty = baseDailyDemand.multiply(BigDecimal.valueOf(forecastDays));
        long qty = baseQty
            .multiply(BigDecimal.ONE.add(demandChangeRate))
            .max(BigDecimal.ZERO)
            .setScale(0, RoundingMode.HALF_UP)
            .longValue();
        return new DemandEstimate(qty, demandChangeRate);
    }

    public BigDecimal newPriceFromChange(BigDecimal currentPrice, int priceChangePercent) {
        return newPriceFromChangeRate(currentPrice, BigDecimal.valueOf(priceChangePercent));
    }

    public BigDecimal newPriceFromChangeRate(
        BigDecimal currentPrice,
        BigDecimal priceChangePercent
    ) {
        BigDecimal rate = priceChangePercent
            .divide(ONE_HUNDRED, CALCULATION_SCALE, RoundingMode.HALF_UP);
        return currentPrice
            .multiply(BigDecimal.ONE.add(rate))
            .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal newPriceFromDiscount(BigDecimal currentPrice, BigDecimal discountPct) {
        BigDecimal rate = discountPct.divide(ONE_HUNDRED, CALCULATION_SCALE, RoundingMode.HALF_UP);
        return currentPrice
            .multiply(BigDecimal.ONE.subtract(rate))
            .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public DssProfitBreakdownResponse profitAt(
        BigDecimal unitPrice,
        BigDecimal unitCost,
        long quantity
    ) {
        return profitCalculator.calculate(unitPrice, unitCost, quantity);
    }

    public BigDecimal profitChangePercent(BigDecimal current, BigDecimal expected) {
        if (current == null || current.compareTo(BigDecimal.ZERO) == 0) {
            return expected.compareTo(BigDecimal.ZERO) > 0
                ? ONE_HUNDRED
                : BigDecimal.ZERO;
        }
        return expected.subtract(current)
            .divide(current.abs(), CALCULATION_SCALE, RoundingMode.HALF_UP)
            .multiply(ONE_HUNDRED)
            .setScale(1, RoundingMode.HALF_UP);
    }

    public String forecastPeriodLabel(int days) {
        return "Kỳ dự báo: " + days + " ngày tới";
    }

    public String scenarioAssumptionNote() {
        return "Dải kịch bản % thay đổi giá: "
            + dssProperties.getPriceChangePercentages()
            + " (cấu hình app.dss.price-change-percentages — không khẳng định tối ưu thống kê).";
    }

    public int defaultForecastDays() {
        return dssProperties.getDefaultForecastDays();
    }
}

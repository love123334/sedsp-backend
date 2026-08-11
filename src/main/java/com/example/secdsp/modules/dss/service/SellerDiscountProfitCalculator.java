package com.example.secdsp.modules.dss.service;

import com.example.secdsp.common.exception.BusinessException;
import com.example.secdsp.config.DssProperties;
import com.example.secdsp.modules.dss.dto.response.DssProfitBreakdownResponse;
import com.example.secdsp.modules.dss.dto.response.SellerDiscountAnalysisResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@RequiredArgsConstructor
public class SellerDiscountProfitCalculator {

    private static final int MONEY_SCALE = 2;
    private static final int RATE_SCALE = 6;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final DssProperties dssProperties;
    private final DssProfitCalculator profitCalculator;
    private final DssScenarioEngine scenarioEngine;

    public SellerDiscountAnalysisResponse calculate(
        BigDecimal currentPrice,
        BigDecimal costPrice,
        BigDecimal priceChangePercent,
        double forecastDemandValue,
        double elasticityValue,
        int simulationPeriod,
        String historicalPeriodLabel,
        String methodology
    ) {
        int maxPct = dssProperties.getMaxPriceChangePercent();
        if (priceChangePercent.compareTo(BigDecimal.valueOf(-maxPct)) < 0
            || priceChangePercent.compareTo(BigDecimal.valueOf(maxPct)) > 0) {
            throw new BusinessException(
                "Mức thay đổi giá phải trong khoảng ±" + maxPct + "%."
            );
        }

        BigDecimal changeRate = priceChangePercent.divide(
            ONE_HUNDRED,
            RATE_SCALE,
            RoundingMode.HALF_UP
        );
        BigDecimal newPrice = scenarioEngine.newPriceFromChangeRate(
            currentPrice,
            priceChangePercent
        );

        if (newPrice.compareTo(costPrice) <= 0) {
            throw new BusinessException(
                "Giá sau điều chỉnh phải lớn hơn giá vốn của sản phẩm."
            );
        }

        long forecastDemand = roundQuantity(
            BigDecimal.valueOf(forecastDemandValue)
        );
        BigDecimal elasticity = BigDecimal.valueOf(elasticityValue);
        BigDecimal demandGrowthRate = elasticity.multiply(changeRate);
        long predictedDemand = roundQuantity(
            BigDecimal.valueOf(forecastDemand)
                .multiply(BigDecimal.ONE.add(demandGrowthRate))
                .max(BigDecimal.ZERO)
        );

        DssProfitBreakdownResponse currentBreakdown = profitCalculator.calculate(
            currentPrice,
            costPrice,
            forecastDemand
        );
        DssProfitBreakdownResponse expectedBreakdown = profitCalculator.calculate(
            newPrice,
            costPrice,
            predictedDemand
        );

        BigDecimal currentProfit = currentBreakdown.getNetProfit();
        BigDecimal expectedProfit = expectedBreakdown.getNetProfit();
        BigDecimal newProfitPerUnit = newPrice.subtract(costPrice);

        long breakEvenQuantity = newProfitPerUnit.compareTo(BigDecimal.ZERO) > 0
            ? currentProfit
                .divide(newProfitPerUnit, 0, RoundingMode.UP)
                .longValue()
            : 0L;
        long additionalUnitsRequired = Math.max(
            breakEvenQuantity - forecastDemand,
            0L
        );

        BigDecimal profitChangePercent = scenarioEngine.profitChangePercent(
            currentProfit,
            expectedProfit
        );

        BigDecimal discountDisplay = priceChangePercent.compareTo(BigDecimal.ZERO) < 0
            ? priceChangePercent.abs()
            : BigDecimal.ZERO;

        String insight = buildBusinessInsight(currentProfit, expectedProfit);
        String recommendation = buildRecommendation(priceChangePercent, simulationPeriod, expectedProfit, currentProfit);
        String reason = buildReason(
            priceChangePercent,
            elasticity,
            forecastDemand,
            predictedDemand,
            simulationPeriod,
            methodology
        );

        return SellerDiscountAnalysisResponse.builder()
            .currentPrice(money(currentPrice))
            .costPrice(money(costPrice))
            .priceChangePercent(priceChangePercent.setScale(2, RoundingMode.HALF_UP))
            .discountPercentage(discountDisplay.setScale(2, RoundingMode.HALF_UP))
            .newPrice(newPrice)
            .forecastDemand(forecastDemand)
            .predictedDemand(predictedDemand)
            .currentProfit(currentProfit)
            .expectedProfit(expectedProfit)
            .breakEvenQuantity(breakEvenQuantity)
            .additionalUnitsRequired(additionalUnitsRequired)
            .businessInsight(insight)
            .simulationPeriod(simulationPeriod)
            .historicalPeriodLabel(historicalPeriodLabel)
            .forecastPeriodLabel(scenarioEngine.forecastPeriodLabel(simulationPeriod))
            .methodology(methodology)
            .currentRevenue(currentBreakdown.getRevenue())
            .expectedRevenue(expectedBreakdown.getRevenue())
            .profitChangePercent(profitChangePercent)
            .currentProfitBreakdown(currentBreakdown)
            .expectedProfitBreakdown(expectedBreakdown)
            .recommendation(recommendation)
            .recommendationReason(reason)
            .build();
    }

    private String buildRecommendation(
        BigDecimal changePct,
        int simulationPeriod,
        BigDecimal expectedProfit,
        BigDecimal currentProfit
    ) {
        int cmp = changePct.compareTo(BigDecimal.ZERO);
        if (cmp == 0) {
            return "Khuyến nghị: giữ giá hiện tại trong " + simulationPeriod + " ngày.";
        }
        String direction = cmp < 0 ? "giảm" : "tăng";
        String profitHint = expectedProfit.compareTo(currentProfit) > 0
            ? " — lợi nhuận ròng dự kiến tăng."
            : expectedProfit.compareTo(currentProfit) < 0
                ? " — lợi nhuận ròng dự kiến giảm, cân nhắc kỹ."
                : " — lợi nhuận ròng dự kiến tương đương.";
        return "Khuyến nghị mô phỏng: "
            + direction
            + " giá "
            + changePct.abs().stripTrailingZeros().toPlainString()
            + "% trong "
            + simulationPeriod
            + " ngày"
            + profitHint;
    }

    private String buildReason(
        BigDecimal changePct,
        BigDecimal elasticity,
        long forecastDemand,
        long predictedDemand,
        int simulationPeriod,
        String methodology
    ) {
        String action = changePct.compareTo(BigDecimal.ZERO) < 0
            ? "Giảm " + changePct.abs().stripTrailingZeros().toPlainString() + "%"
            : changePct.compareTo(BigDecimal.ZERO) > 0
                ? "Tăng " + changePct.stripTrailingZeros().toPlainString() + "%"
                : "Giữ giá";
        return action
            + ", co giãn "
            + elasticity.setScale(2, RoundingMode.HALF_UP)
            + " → nhu cầu "
            + forecastDemand
            + " → "
            + predictedDemand
            + " SP trong "
            + simulationPeriod
            + " ngày. "
            + methodology;
    }

    private String buildBusinessInsight(
        BigDecimal currentProfit,
        BigDecimal expectedProfit
    ) {
        BigDecimal tolerance = dssProperties.getProfitTolerancePercent()
            .divide(ONE_HUNDRED, RATE_SCALE, RoundingMode.HALF_UP);

        if (currentProfit.compareTo(BigDecimal.ZERO) == 0) {
            return expectedProfit.compareTo(BigDecimal.ZERO) > 0
                ? "Kịch bản dự kiến sẽ làm tăng tổng lợi nhuận ròng."
                : "Kịch bản có khả năng duy trì mức lợi nhuận hiện tại.";
        }

        BigDecimal profitChangeRate = expectedProfit
            .subtract(currentProfit)
            .divide(currentProfit.abs(), RATE_SCALE, RoundingMode.HALF_UP);

        if (profitChangeRate.abs().compareTo(tolerance) <= 0) {
            return "Kịch bản có khả năng duy trì mức lợi nhuận ròng hiện tại.";
        }

        if (expectedProfit.compareTo(currentProfit) > 0) {
            return "Kịch bản dự kiến sẽ làm tăng tổng lợi nhuận ròng.";
        }

        return "Kịch bản có thể làm giảm lợi nhuận ròng nếu không đạt thêm doanh số cần thiết.";
    }

    private long roundQuantity(BigDecimal quantity) {
        return quantity
            .max(BigDecimal.ZERO)
            .setScale(0, RoundingMode.HALF_UP)
            .longValue();
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}

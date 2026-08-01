package com.example.secdsp.modules.dss.service;

import com.example.secdsp.common.exception.BusinessException;
import com.example.secdsp.modules.dss.dto.response.SellerDiscountAnalysisResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class SellerDiscountProfitCalculator {

    private static final int MONEY_SCALE = 2;
    private static final int RATE_SCALE = 6;
    private static final BigDecimal ONE_HUNDRED =
        BigDecimal.valueOf(100);
    private static final BigDecimal PROFIT_TOLERANCE_RATE =
        new BigDecimal("0.03");

    private static final String PROFIT_INCREASE_INSIGHT =
        "Chiến lược giảm giá dự kiến sẽ làm tăng tổng lợi nhuận.";
    private static final String PROFIT_MAINTAIN_INSIGHT =
        "Chiến lược giảm giá có khả năng duy trì mức lợi nhuận hiện tại.";
    private static final String PROFIT_DECREASE_INSIGHT =
        "Mức giảm giá đề xuất có thể làm giảm lợi nhuận nếu không đạt thêm doanh số cần thiết.";

    public SellerDiscountAnalysisResponse calculate(
        BigDecimal currentPrice,
        BigDecimal costPrice,
        BigDecimal discountPercentage,
        double forecastDemandValue,
        double elasticityValue
    ) {
        BigDecimal discountRate = discountPercentage.divide(
            ONE_HUNDRED,
            RATE_SCALE,
            RoundingMode.HALF_UP
        );
        BigDecimal newPrice = money(
            currentPrice.multiply(BigDecimal.ONE.subtract(discountRate))
        );

        if (newPrice.compareTo(costPrice) <= 0) {
            throw new BusinessException(
                "Giá sau giảm phải lớn hơn giá vốn của sản phẩm."
            );
        }

        long forecastDemand = roundQuantity(forecastDemandValue);
        BigDecimal elasticity = BigDecimal.valueOf(elasticityValue);
        BigDecimal demandGrowthRate = elasticity
            .multiply(discountRate.negate());
        BigDecimal predictedDemandValue = BigDecimal
            .valueOf(forecastDemand)
            .multiply(BigDecimal.ONE.add(demandGrowthRate))
            .max(BigDecimal.ZERO);
        long predictedDemand = roundQuantity(predictedDemandValue);

        BigDecimal currentProfitPerUnit = currentPrice
            .subtract(costPrice);
        BigDecimal newProfitPerUnit = newPrice
            .subtract(costPrice);
        BigDecimal currentProfit = money(
            currentProfitPerUnit.multiply(
                BigDecimal.valueOf(forecastDemand)
            )
        );
        BigDecimal expectedProfit = money(
            newProfitPerUnit.multiply(
                BigDecimal.valueOf(predictedDemand)
            )
        );
        long breakEvenQuantity = currentProfit
            .divide(newProfitPerUnit, 0, RoundingMode.HALF_UP)
            .longValue();
        long additionalUnitsRequired = Math.max(
            breakEvenQuantity - forecastDemand,
            0L
        );

        return SellerDiscountAnalysisResponse.builder()
            .currentPrice(money(currentPrice))
            .costPrice(money(costPrice))
            .discountPercentage(discountPercentage)
            .newPrice(newPrice)
            .forecastDemand(forecastDemand)
            .predictedDemand(predictedDemand)
            .currentProfit(currentProfit)
            .expectedProfit(expectedProfit)
            .breakEvenQuantity(breakEvenQuantity)
            .additionalUnitsRequired(additionalUnitsRequired)
            .businessInsight(buildBusinessInsight(
                currentProfit,
                expectedProfit
            ))
            .build();
    }

    private String buildBusinessInsight(
        BigDecimal currentProfit,
        BigDecimal expectedProfit
    ) {
        if (currentProfit.compareTo(BigDecimal.ZERO) == 0) {
            return expectedProfit.compareTo(BigDecimal.ZERO) > 0
                ? PROFIT_INCREASE_INSIGHT
                : PROFIT_MAINTAIN_INSIGHT;
        }

        BigDecimal profitChangeRate = expectedProfit
            .subtract(currentProfit)
            .divide(
                currentProfit.abs(),
                RATE_SCALE,
                RoundingMode.HALF_UP
            );

        if (profitChangeRate.abs()
            .compareTo(PROFIT_TOLERANCE_RATE) <= 0) {
            return PROFIT_MAINTAIN_INSIGHT;
        }

        if (expectedProfit.compareTo(currentProfit) > 0) {
            return PROFIT_INCREASE_INSIGHT;
        }

        return PROFIT_DECREASE_INSIGHT;
    }

    private long roundQuantity(double quantity) {
        return roundQuantity(BigDecimal.valueOf(quantity));
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

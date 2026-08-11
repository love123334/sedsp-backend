package com.example.secdsp.modules.dss.service;

import com.example.secdsp.config.DssProperties;
import com.example.secdsp.modules.dss.dto.response.DssProfitBreakdownResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DssProfitCalculator {

    private static final int MONEY_SCALE = 2;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final DssProperties dssProperties;

    public DssProfitBreakdownResponse calculate(
        BigDecimal unitPrice,
        BigDecimal unitCost,
        long quantity
    ) {
        BigDecimal qty = BigDecimal.valueOf(Math.max(quantity, 0));
        BigDecimal revenue = money(unitPrice.multiply(qty));
        BigDecimal cogs = money(unitCost.multiply(qty));

        List<String> notes = new ArrayList<>();
        notes.add("Giá vốn (COGS): lấy từ products.cost_price × số lượng.");

        BigDecimal deliveryCost = BigDecimal.ZERO;
        if (dssProperties.isIncludeShippingInProfit()) {
            deliveryCost = money(
                dssProperties.getAvgShippingPerUnitVnd().multiply(qty)
            );
            notes.add(
                "Chi phí giao hàng: ước tính "
                    + dssProperties.getAvgShippingPerUnitVnd()
                    + " VND/SP (cấu hình app.dss.avg-shipping-per-unit-vnd; "
                    + "orders.shipping_fee hiện theo đơn, chưa phân bổ theo SP)."
            );
        } else {
            notes.add("Chi phí giao hàng: chưa trừ (include-shipping-in-profit=false).");
        }

        BigDecimal platformFee = BigDecimal.ZERO;
        if (dssProperties.isIncludePlatformFee()) {
            platformFee = money(
                revenue.multiply(dssProperties.getPlatformFeePercent())
                    .divide(ONE_HUNDRED, MONEY_SCALE, RoundingMode.HALF_UP)
            );
            notes.add(
                "Phí nền tảng: "
                    + dssProperties.getPlatformFeePercent()
                    + "% doanh thu (cấu hình; không có bảng phí riêng trong DB)."
            );
        } else {
            notes.add("Phí nền tảng: chưa trừ.");
        }

        BigDecimal operatingCost = BigDecimal.ZERO;
        if (dssProperties.getOperatingCostPerUnitVnd()
            .compareTo(BigDecimal.ZERO) > 0) {
            operatingCost = money(
                dssProperties.getOperatingCostPerUnitVnd().multiply(qty)
            );
            notes.add(
                "Chi phí vận hành: "
                    + dssProperties.getOperatingCostPerUnitVnd()
                    + " VND/SP (cấu hình app.dss.operating-cost-per-unit-vnd)."
            );
        } else {
            notes.add(
                "Chi phí vận hành: 0 VND/SP (chưa cấu hình — có thể set app.dss.operating-cost-per-unit-vnd)."
            );
        }

        BigDecimal grossProfit = money(revenue.subtract(cogs));
        BigDecimal netProfit = money(
            revenue
                .subtract(cogs)
                .subtract(deliveryCost)
                .subtract(platformFee)
                .subtract(operatingCost)
        );

        return DssProfitBreakdownResponse.builder()
            .revenue(revenue)
            .costOfGoodsSold(cogs)
            .deliveryCost(deliveryCost)
            .platformFee(platformFee)
            .operatingCost(operatingCost)
            .grossProfit(grossProfit)
            .netProfit(netProfit)
            .costNotes(notes)
            .build();
    }

    public BigDecimal netProfitPerUnit(
        BigDecimal unitPrice,
        BigDecimal unitCost
    ) {
        return calculate(unitPrice, unitCost, 1).getNetProfit();
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}

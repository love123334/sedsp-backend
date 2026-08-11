package com.example.secdsp.modules.dss.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DssProfitBreakdownResponse {

    BigDecimal revenue;

    BigDecimal costOfGoodsSold;

    BigDecimal deliveryCost;

    BigDecimal platformFee;

    BigDecimal operatingCost;

    /** Doanh thu − giá vốn (chưa trừ phí giao hàng / nền tảng). */
    BigDecimal grossProfit;

    /** Lợi nhuận ròng sau các khoản trừ cấu hình. */
    BigDecimal netProfit;

    /** Ghi chú: khoản nào từ DB, khoản nào cấu hình / ước tính. */
    List<String> costNotes;
}

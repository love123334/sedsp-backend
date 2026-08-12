package com.example.secdsp.modules.dss.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DssProductContextResponse {

    /** Ngày đăng sản phẩm trên shop */
    OffsetDateTime listedAt;

    Integer daysListed;

    /** Ngày bán giao thành công đầu tiên */
    LocalDate firstSaleDate;

    Integer daysSinceFirstSale;

    /** Số lần chỉnh giá trong kỳ phân tích */
    Integer priceChangeCount;

    /** Xếp hạng trong shop theo số lượng bán (1 = bán chạy nhất) */
    Integer shopSalesRank;

    Integer shopProductCount;

    /** TOP | MID | LOW | ONLY */
    String performanceTier;

    String performanceSummary;
}

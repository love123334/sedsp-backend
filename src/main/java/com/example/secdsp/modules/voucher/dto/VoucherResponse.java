package com.example.secdsp.modules.voucher.dto;

import com.example.secdsp.modules.voucher.entity.*;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Builder
public record VoucherResponse(
    Long id,
    String code,
    String name,
    String description,
    VoucherDiscountType discountType,
    BigDecimal discountValue,
    VoucherScope scope,
    Long sellerId,
    String sellerName,
    VoucherAppliesTo appliesTo,
    BigDecimal minimumOrderAmount,
    BigDecimal maximumDiscountAmount,
    Integer usageLimit,
    Integer usedCount,
    OffsetDateTime startsAt,
    OffsetDateTime endsAt,
    Boolean isActive,
    List<Long> productIds,
    Long requestId,
    OffsetDateTime createdAt
) {}

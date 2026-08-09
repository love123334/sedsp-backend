package com.example.secdsp.modules.voucher.dto;

import com.example.secdsp.modules.voucher.entity.*;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Builder
public record VoucherRequestResponse(
    Long id,
    Long sellerId,
    String sellerName,
    String code,
    String name,
    String description,
    VoucherDiscountType discountType,
    BigDecimal discountValue,
    VoucherAppliesTo appliesTo,
    BigDecimal minimumOrderAmount,
    BigDecimal maximumDiscountAmount,
    Integer usageLimit,
    OffsetDateTime startsAt,
    OffsetDateTime endsAt,
    VoucherRequestStatus status,
    String managerNote,
    Long voucherId,
    List<Long> productIds,
    OffsetDateTime createdAt,
    OffsetDateTime reviewedAt
) {}

package com.example.secdsp.modules.voucher.dto;

import com.example.secdsp.modules.voucher.entity.VoucherDiscountType;
import com.example.secdsp.modules.voucher.entity.VoucherScope;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record ValidateVoucherResponse(
    boolean valid,
    String message,
    Long voucherId,
    String code,
    String name,
    String description,
    VoucherDiscountType discountType,
    BigDecimal discountValue,
    VoucherScope scope,
    Long sellerId,
    String sellerName,
    BigDecimal discountAmount,
    BigDecimal eligibleSubtotal
) {}

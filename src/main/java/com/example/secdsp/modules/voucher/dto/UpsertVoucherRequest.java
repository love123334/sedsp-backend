package com.example.secdsp.modules.voucher.dto;

import com.example.secdsp.modules.voucher.entity.VoucherAppliesTo;
import com.example.secdsp.modules.voucher.entity.VoucherDiscountType;
import com.example.secdsp.modules.voucher.entity.VoucherScope;
import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpsertVoucherRequest {

    @NotBlank
    @Size(max = 50)
    String code;

    @NotBlank
    @Size(max = 255)
    String name;

    String description;

    @NotNull
    VoucherDiscountType discountType;

    @NotNull
    @DecimalMin("0.01")
    BigDecimal discountValue;

    @NotNull
    VoucherScope scope;

    Long sellerId;

    @NotNull
    VoucherAppliesTo appliesTo;

    @DecimalMin("0")
    BigDecimal minimumOrderAmount = BigDecimal.ZERO;

    @DecimalMin("0")
    BigDecimal maximumDiscountAmount;

    @Min(1)
    Integer usageLimit;

    @NotNull
    OffsetDateTime startsAt;

    @NotNull
    OffsetDateTime endsAt;

    List<Long> productIds;
}

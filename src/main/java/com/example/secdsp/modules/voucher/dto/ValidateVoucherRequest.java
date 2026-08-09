package com.example.secdsp.modules.voucher.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ValidateVoucherRequest {

    @NotBlank
    String code;

    /** Product ids currently in cart */
    List<Long> productIds;
}

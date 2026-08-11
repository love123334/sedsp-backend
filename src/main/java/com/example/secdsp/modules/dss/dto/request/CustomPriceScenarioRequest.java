package com.example.secdsp.modules.dss.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomPriceScenarioRequest {

    @NotNull
    @Positive
    Long productId;

    @NotNull
    LocalDate fromDate;

    @NotNull
    LocalDate toDate;

    /** Giá bán tùy chỉnh (VND) — hệ thống suy ra % thay đổi so với giá hiện tại. */
    @NotNull
    @DecimalMin(value = "0.01", message = "Giá tùy chỉnh phải lớn hơn 0")
    BigDecimal customPrice;
}

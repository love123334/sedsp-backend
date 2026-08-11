package com.example.secdsp.modules.dss.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SellerDiscountAnalysisRequest {

    @NotNull(message = "Product ID is required")
    @Positive(message = "Product ID must be greater than 0")
    Long productId;

    /**
     * % thay đổi giá: âm = giảm, 0 = giữ, dương = tăng. Phạm vi ±300%.
     */
    @NotNull(message = "Price change percent is required")
    @DecimalMin(value = "-300", message = "Price change must be at least -300%")
    @DecimalMax(value = "300", message = "Price change must be at most 300%")
    BigDecimal priceChangePercent;

    @NotNull(message = "Simulation period is required")
    @Positive(message = "Simulation period must be greater than 0")
    Integer simulationPeriod;
}

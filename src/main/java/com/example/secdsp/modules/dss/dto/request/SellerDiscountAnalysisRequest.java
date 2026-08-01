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

    @NotNull(message = "Discount percentage is required")
    @DecimalMin(
        value = "0.01",
        message = "Discount percentage must be greater than 0"
    )
    @DecimalMax(
        value = "99.99",
        message = "Discount percentage must be less than 100"
    )
    BigDecimal discountPercentage;

    @NotNull(message = "Simulation period is required")
    @Positive(message = "Simulation period must be greater than 0")
    Integer simulationPeriod;
}

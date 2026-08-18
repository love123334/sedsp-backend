package com.example.secdsp.modules.dss.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateAdvancedPriceScenarioRequest {

    @NotNull(message = "Price change percentage is required")
    @DecimalMin(
        value = "-70.00",
        message = "Price change percentage must be greater than or equal to -70"
    )
    @DecimalMax(
        value = "100.00",
        message = "Price change percentage must be less than or equal to 100"
    )
    BigDecimal priceChangePercent;
}


package com.example.secdsp.modules.dss.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateAdvancedPriceSessionRequest {

    @NotNull(message = "Product ID is required")
    @Positive(message = "Product ID must be greater than 0")
    Long productId;

    @NotNull(message = "From date is required")
    LocalDate fromDate;

    @NotNull(message = "To date is required")
    LocalDate toDate;

    @NotNull(message = "Forecast period is required")
    @Positive(message = "Forecast period must be greater than 0")
    Integer forecastPeriod;

    @NotNull(message = "Estimated order cost is required")
    @DecimalMin(
        value = "0.00",
        message = "Estimated order cost must be greater than or equal to 0"
    )
    BigDecimal estimatedOrderCost;
}


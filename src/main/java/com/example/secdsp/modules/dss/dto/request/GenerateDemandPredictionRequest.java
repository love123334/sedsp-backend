package com.example.secdsp.modules.dss.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GenerateDemandPredictionRequest {

    @NotNull(message = "Product ID is required")
    @Positive(message = "Product ID must be greater than 0")
    Long productId;

    @NotNull(message = "Forecast period is required")
    @Positive(message = "Forecast period must be greater than 0")
    Integer forecastPeriod;

    @NotNull(message = "Historical days is required")
    @Positive(message = "Historical days must be greater than 0")
    Integer historicalDays;
}

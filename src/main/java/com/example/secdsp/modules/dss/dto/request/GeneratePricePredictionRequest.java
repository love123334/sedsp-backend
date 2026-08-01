package com.example.secdsp.modules.dss.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GeneratePricePredictionRequest {

    @NotNull(message = "Product ID is required")
    @Positive(message = "Product ID must be greater than 0")
    Long productId;

    @NotNull(message = "From date is required")
    LocalDate fromDate;

    @NotNull(message = "To date is required")
    LocalDate toDate;
}

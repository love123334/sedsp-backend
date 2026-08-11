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
public class TargetProfitAnalysisRequest {

    @NotNull
    @Positive
    Long productId;

    @NotNull
    @Positive
    BigDecimal targetProfitVnd;

    @NotNull
    @Positive
    Integer simulationPeriod;
}

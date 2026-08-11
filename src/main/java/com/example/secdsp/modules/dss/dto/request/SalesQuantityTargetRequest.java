package com.example.secdsp.modules.dss.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SalesQuantityTargetRequest {

    @NotNull
    @Positive
    Long productId;

    /** % tăng số lượng bán mong muốn so với dự báo hiện tại, vd. 20 = +20%. */
    @NotNull
    @DecimalMin("0.01")
    @DecimalMax("500")
    Double increasePercent;

    @NotNull
    @Positive
    Integer simulationPeriod;
}

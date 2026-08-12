package com.example.secdsp.modules.dss.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DssHolidayImpactResponse {

    String code;

    String label;

    LocalDate start;

    LocalDate end;

    BigDecimal demandMultiplier;

    String note;
}

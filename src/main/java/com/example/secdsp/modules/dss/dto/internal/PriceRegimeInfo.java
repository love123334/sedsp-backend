package com.example.secdsp.modules.dss.dto.internal;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PriceRegimeInfo(
    BigDecimal price,
    LocalDate fromDate,
    LocalDate toDate,
    long quantitySold,
    BigDecimal averageDailyDemand
) {}

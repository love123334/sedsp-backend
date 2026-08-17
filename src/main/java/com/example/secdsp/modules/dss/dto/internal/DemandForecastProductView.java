package com.example.secdsp.modules.dss.dto.internal;

import java.math.BigDecimal;

public record DemandForecastProductView(
    Long productId,
    Long sellerId,
    String productName,
    BigDecimal currentPrice
) {}

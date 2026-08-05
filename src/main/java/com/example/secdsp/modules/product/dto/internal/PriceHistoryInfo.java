package com.example.secdsp.modules.product.dto.internal;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PriceHistoryInfo(
    BigDecimal oldPrice,
    BigDecimal newPrice,
    OffsetDateTime changedAt
) {}

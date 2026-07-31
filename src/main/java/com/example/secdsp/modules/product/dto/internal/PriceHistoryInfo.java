package com.example.secdsp.modules.product.dto.internal;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PriceHistoryInfo(
    BigDecimal oldPrice,
    BigDecimal newPrice,
    LocalDateTime changedAt
) {}

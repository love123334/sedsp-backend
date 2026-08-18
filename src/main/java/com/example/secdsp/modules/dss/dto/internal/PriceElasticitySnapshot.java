package com.example.secdsp.modules.dss.dto.internal;

import java.math.BigDecimal;

public record PriceElasticitySnapshot(
    BigDecimal averageElasticity,
    long quantitySold
) {
}

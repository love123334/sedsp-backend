package com.example.secdsp.modules.product.repository.projection;

import java.math.BigDecimal;

/** Scalar pricing row for voucher cart math (avoids Object[] / Tuple JDBC quirks). */
public interface ProductPricingRow {

    Long getId();

    Long getSellerId();

    BigDecimal getPrice();
}

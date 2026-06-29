package com.example.secdsp.modules.product.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PriceHistoryResponse {

    Long id;
    BigDecimal oldPrice;
    BigDecimal newPrice;
    LocalDateTime changedAt;
}

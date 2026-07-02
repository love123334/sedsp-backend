package com.example.secdsp.modules.sellerperformance.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Builder
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TopProductResponse {

    Long productId;

    String productName;

    Long quantitySold;

    BigDecimal revenue;
}

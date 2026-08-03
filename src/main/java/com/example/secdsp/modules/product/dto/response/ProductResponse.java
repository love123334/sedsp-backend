package com.example.secdsp.modules.product.dto.response;

import com.example.secdsp.modules.product.entity.ProductStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductResponse {
    Long id;
    String name;
    String slug;
    BigDecimal price;
    ProductStatus status;
    Long categoryId;
    String categoryName;
    String sellerStoreName;
    OffsetDateTime createdAt;
}

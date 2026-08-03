package com.example.secdsp.modules.product.dto.response;

import com.example.secdsp.modules.product.entity.ProductStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductDetailResponse {
    Long id;
    String name;
    String slug;
    String description;
    BigDecimal price;
    BigDecimal costPrice;
    ProductStatus status;
    Long categoryId;
    String categoryName;
    Long sellerId;
    String sellerStoreName;
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
    List<ProductImageResponse> images;
    List<ProductAttributeResponse> attributes;
}

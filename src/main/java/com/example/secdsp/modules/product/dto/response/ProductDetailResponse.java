package com.example.secdsp.modules.product.dto.response;

import com.example.secdsp.modules.product.entity.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Detailed product information")
public class ProductDetailResponse {

    @Schema(description = "Product identifier", example = "1")
    Long id;

    @Schema(description = "Product name", example = "iPhone 16 Pro")
    String name;

    @Schema(description = "SEO-friendly slug", example = "iphone-16-pro")
    String slug;

    @Schema(
        description = "Detailed product description",
        example = "Apple iPhone 16 Pro powered by the A18 Pro chip."
    )
    String description;

    @Schema(description = "Selling price", example = "29990000")
    BigDecimal price;

    @Schema(description = "Cost price", example = "25000000")
    BigDecimal costPrice;

    @Schema(
        description = "Current product status",
        implementation = ProductStatus.class
    )
    ProductStatus status;

    @Schema(description = "Category identifier", example = "3")
    Long categoryId;

    @Schema(description = "Category name", example = "Smartphones")
    String categoryName;

    @Schema(description = "Seller identifier", example = "12")
    Long sellerId;

    @Schema(description = "Seller store name", example = "Apple Store")
    String sellerStoreName;

    @Schema(description = "Available sellable stock quantity", example = "42")
    Integer availableQuantity;

    @Schema(
        description = "Creation timestamp",
        example = "2026-08-03T10:00:00Z"
    )
    OffsetDateTime createdAt;

    @Schema(
        description = "Last update timestamp",
        example = "2026-08-03T12:30:00Z"
    )
    OffsetDateTime updatedAt;

    @Schema(description = "Product images")
    List<ProductImageResponse> images;

    @Schema(description = "Product attributes")
    List<ProductAttributeResponse> attributes;
}
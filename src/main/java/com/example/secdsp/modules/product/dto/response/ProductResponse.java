package com.example.secdsp.modules.product.dto.response;

import com.example.secdsp.modules.product.entity.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Product summary information")
public class ProductResponse {

    @Schema(description = "Product identifier", example = "1")
    Long id;

    @Schema(description = "Product name", example = "iPhone 16 Pro")
    String name;

    @Schema(description = "SEO-friendly slug", example = "iphone-16-pro")
    String slug;

    @Schema(description = "Selling price", example = "29990000")
    BigDecimal price;

    @Schema(
        description = "Current product status",
        implementation = ProductStatus.class
    )
    ProductStatus status;

    @Schema(description = "Category identifier", example = "3")
    Long categoryId;

    @Schema(description = "Category name", example = "Smartphones")
    String categoryName;

    @Schema(description = "Seller identifier", example = "5")
    Long sellerId;

    @Schema(description = "Seller store name", example = "Apple Store")
    String sellerStoreName;

    @Schema(description = "Seller contact email", example = "seller@example.com")
    String sellerEmail;

    @Schema(description = "Seller contact phone", example = "0901234567")
    String sellerPhone;

    @Schema(description = "Primary product image for catalog cards")
    String primaryImageUrl;

    @Schema(description = "Available sellable stock quantity", example = "42")
    Integer availableQuantity;

    @Schema(
        description = "Creation timestamp",
        example = "2026-08-03T10:30:00Z"
    )
    OffsetDateTime createdAt;

    @Schema(description = "Average customer rating (1-5)", example = "4.6")
    Double averageRating;

    @Schema(description = "Number of published reviews", example = "12")
    Long reviewCount;

    @Schema(description = "Units sold from paid/delivered orders", example = "48")
    Long soldCount;
}

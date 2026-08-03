package com.example.secdsp.modules.product.dto.request;

import com.example.secdsp.modules.product.entity.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Request to update a product")
public class UpdateProductRequest {

    @Schema(
        description = "Product name",
        example = "iPhone 16 Pro Max"
    )
    String name;

    @Schema(
        description = "SEO-friendly slug",
        example = "iphone-16-pro-max"
    )
    String slug;

    @Schema(
        description = "Detailed product description"
    )
    String description;

    @Schema(
        description = "Selling price",
        example = "31990000"
    )
    @DecimalMin(value = "0.0")
    BigDecimal price;

    @Schema(
        description = "Cost price",
        example = "27000000"
    )
    @DecimalMin(value = "0.0")
    BigDecimal costPrice;

    @Schema(
        description = "Product status",
        implementation = ProductStatus.class
    )
    ProductStatus status;

    @Schema(
        description = "Category identifier",
        example = "3"
    )
    Long categoryId;

    @Schema(description = "Updated product images")
    @Valid
    List<UpdateProductImageRequest> images;

    @Schema(description = "Updated product attributes")
    @Valid
    List<UpdateProductAttributeRequest> attributes;
}

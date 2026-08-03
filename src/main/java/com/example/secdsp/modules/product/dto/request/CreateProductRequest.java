package com.example.secdsp.modules.product.dto.request;

import com.example.secdsp.modules.product.entity.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@Schema(description = "Request to create a product")
public class CreateProductRequest {

    @Schema(
        description = "Product name",
        example = "iPhone 16 Pro",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Product name cannot be blank")
    String name;

    @Schema(
        description = "SEO-friendly slug",
        example = "iphone-16-pro"
    )
    String slug;

    @Schema(
        description = "Detailed product description",
        example = "Apple iPhone 16 Pro with A18 Pro chip."
    )
    String description;

    @Schema(
        description = "Selling price",
        example = "29990000",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull
    @DecimalMin(value = "0.0")
    BigDecimal price;

    @Schema(
        description = "Cost price",
        example = "25000000"
    )
    @DecimalMin(value = "0.0")
    BigDecimal costPrice;

    @Schema(
        description = "Product status",
        implementation = ProductStatus.class
    )
    ProductStatus status = ProductStatus.ACTIVE;

    @Schema(
        description = "Category identifier",
        example = "3"
    )
    Long categoryId;

    @Schema(description = "Product images")
    @Valid
    List<AddProductImageRequest> images;

    @Schema(description = "Product attributes")
    @Valid
    List<AddProductAttributeRequest> attributes;
}

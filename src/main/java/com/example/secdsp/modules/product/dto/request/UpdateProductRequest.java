package com.example.secdsp.modules.product.dto.request;

import com.example.secdsp.modules.product.entity.ProductStatus;
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
public class UpdateProductRequest {

    String name;

    String slug;

    String description;

    @DecimalMin(value = "0.0", inclusive = true, message = "Product price must be non-negative")
    BigDecimal price;

    @DecimalMin(value = "0.0", inclusive = true, message = "Product cost price must be non-negative")
    BigDecimal costPrice;

    ProductStatus status;

    Long categoryId;

    Long brandId;

    @Valid
    List<UpdateProductImageRequest> images;

    @Valid
    List<UpdateProductAttributeRequest> attributes;
}

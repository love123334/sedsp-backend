package com.example.secdsp.modules.product.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Product image")
public class ProductImageResponse {

    @Schema(description = "Image identifier", example = "15")
    Long id;

    @Schema(
        description = "Image URL",
        example = "https://cdn.example.com/products/iphone-front.jpg"
    )
    String imageUrl;

    @Schema(
        description = "Whether this is the primary image",
        example = "true"
    )
    boolean isPrimary;
}
package com.example.secdsp.modules.product.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Product attribute")
public class ProductAttributeResponse {

    @Schema(description = "Attribute identifier", example = "8")
    Long id;

    @Schema(description = "Attribute name", example = "Color")
    String attributeName;

    @Schema(description = "Attribute value", example = "Black")
    String attributeValue;
}
package com.example.secdsp.modules.product.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Request to add a product attribute")
public class AddProductAttributeRequest {

    @Schema(
        description = "Attribute name",
        example = "Color",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Attribute name cannot be blank")
    @Size(max = 100)
    String attributeName;

    @Schema(
        description = "Attribute value",
        example = "Black",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Attribute value cannot be blank")
    @Size(max = 255)
    String attributeValue;
}
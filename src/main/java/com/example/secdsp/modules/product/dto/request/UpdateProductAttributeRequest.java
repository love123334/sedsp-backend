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
@Schema(description = "Request to create or update a product attribute")
public class UpdateProductAttributeRequest {

    @Schema(
        description = """
            Attribute identifier.
            
            Leave null to create a new attribute.
            Provide an existing id to update an existing attribute.
            """,
        example = "8"
    )
    Long id;

    @Schema(
        description = "Attribute name",
        example = "Storage",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank
    @Size(max = 100)
    String attributeName;

    @Schema(
        description = "Attribute value",
        example = "256 GB",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank
    @Size(max = 255)
    String attributeValue;
}
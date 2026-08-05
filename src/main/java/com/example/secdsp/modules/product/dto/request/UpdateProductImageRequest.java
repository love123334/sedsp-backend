package com.example.secdsp.modules.product.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Request to create or update a product image")
public class UpdateProductImageRequest {

    @Schema(
        description = """
            Image identifier.
            
            Leave null to create a new image.
            Provide an existing id to update an existing image.
            """,
        example = "12"
    )
    Long id;

    @Schema(
        description = "Image URL",
        example = "https://cdn.example.com/products/iphone-front.jpg",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank
    @Pattern(regexp = "^(http|https)://.*")
    String imageUrl;

    @Schema(
        description = "Cloudinary public identifier",
        example = "products/iphone-front",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank
    String publicId;

    @Schema(
        description = "Whether this image is the primary image",
        example = "false"
    )
    @JsonProperty("isPrimary")
    boolean isPrimary = false;
}
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
@Schema(description = "Request to add a product image")
public class AddProductImageRequest {

    @Schema(
        description = "Image URL",
        example = "https://cdn.example.com/products/iphone-front.jpg",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Image URL cannot be blank")
    @Pattern(
        regexp = "^(http|https)://.*",
        message = "Image URL must be a valid URL"
    )
    String imageUrl;

    @Schema(description = "Cloudinary public_id — nếu thiếu sẽ tự sinh khi lưu")
    String publicId;

    @Schema(
        description = "Whether this image is the primary image",
        example = "true"
    )
    @JsonProperty("isPrimary")
    boolean isPrimary = false;
}
package com.example.secdsp.modules.product.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    name = "CloudinaryUploadResult",
    description = "Cloudinary upload result"
)
public record CloudinaryUploadResult(

    @Schema(
        description = "Public image URL",
        example = "https://res.cloudinary.com/demo/image/upload/v123/products/iphone.jpg"
    )
    String url,

    @Schema(
        description = "Cloudinary public identifier",
        example = "products/iphone"
    )
    String publicId
) {}
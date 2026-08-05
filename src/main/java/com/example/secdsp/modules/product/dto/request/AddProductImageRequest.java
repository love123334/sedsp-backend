package com.example.secdsp.modules.product.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class AddProductImageRequest {

    @NotBlank(message = "Image URL cannot be blank")
    @Pattern(regexp = "^(http|https)://.*", message = "Image URL must be a valid URL")
    String imageUrl;

    /** Cloudinary public_id — nếu thiếu sẽ tự sinh khi lưu */
    String publicId;

    @JsonProperty("isPrimary")
    boolean isPrimary = false;
}

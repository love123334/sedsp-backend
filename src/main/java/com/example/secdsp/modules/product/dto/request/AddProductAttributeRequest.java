package com.example.secdsp.modules.product.dto.request;

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
public class AddProductAttributeRequest {

    @NotBlank(message = "Attribute name cannot be blank")
    @Size(max = 100, message = "Attribute name must not exceed 100 characters")
    String attributeName;

    @NotBlank(message = "Attribute value cannot be blank")
    @Size(max = 255, message = "Attribute value must not exceed 255 characters")
    String attributeValue;
}

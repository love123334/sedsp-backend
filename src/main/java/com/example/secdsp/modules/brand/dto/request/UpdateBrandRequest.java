package com.example.secdsp.modules.brand.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateBrandRequest {

    @NotBlank(message = "Brand name cannot be blank")
    @Size(max = 150, message = "Brand name must not exceed 150 characters")
    private String name;

    @NotBlank(message = "Brand slug cannot be blank")
    @Size(max = 150, message = "Brand slug must not exceed 150 characters")
    private String slug;
}

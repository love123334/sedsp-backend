package com.example.secdsp.modules.category.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateCategoryRequest {

    @NotBlank(message = "Category name cannot be blank")
    @Size(max = 150, message = "Category name must not exceed 150 characters")
    private String name;

    @NotBlank(message = "Category slug cannot be blank")
    @Size(max = 150, message = "Category slug must not exceed 150 characters")
    private String slug;

    private Long parentId;
}

package com.example.secdsp.modules.category.dto.request;

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
@Schema(description = "Create category request")
public class CreateCategoryRequest {

    @Schema(
        description = "Category name",
        example = "Running Shoes",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank
    @Size(max = 150)
    String name;

    @Schema(
        description = "Parent category identifier. Leave null for a root category.",
        example = "1",
        nullable = true
    )
    Long parentId;
}

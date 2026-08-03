package com.example.secdsp.modules.category.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Category tree node")
public class CategoryTreeResponse {

    @Schema(
        description = "Category identifier",
        example = "1"
    )
    Long id;

    @Schema(
        description = "Category name",
        example = "Shoes"
    )
    String name;

    @Schema(
        description = "SEO-friendly slug",
        example = "shoes"
    )
    String slug;

    @Builder.Default
    @Schema(description = "Child categories")
    List<CategoryTreeResponse> children = new ArrayList<>();
}
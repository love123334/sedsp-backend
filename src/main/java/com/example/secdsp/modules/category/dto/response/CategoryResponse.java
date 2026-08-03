package com.example.secdsp.modules.category.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Category information")
public class CategoryResponse {

    @Schema(
        description = "Category identifier",
        example = "1"
    )
    Long id;

    @Schema(
        description = "Category name",
        example = "Running Shoes"
    )
    String name;

    @Schema(
        description = "SEO-friendly slug",
        example = "running-shoes"
    )
    String slug;

    @Schema(
        description = "Parent category identifier",
        example = "5",
        nullable = true
    )
    Long parentId;

    @Schema(
        description = "Parent category name",
        example = "Shoes",
        nullable = true
    )
    String parentName;

    @Schema(
        description = "Category creation timestamp",
        example = "2026-08-03T09:30:00Z"
    )
    OffsetDateTime createdAt;
}

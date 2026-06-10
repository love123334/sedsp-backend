package com.example.secdsp.modules.category.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class CategoryResponse {
    private Long id;
    private String name;
    private String slug;
    private Long parentId;
    private String parentName;
    private LocalDateTime createdAt;
}

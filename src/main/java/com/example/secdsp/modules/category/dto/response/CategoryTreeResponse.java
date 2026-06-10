package com.example.secdsp.modules.category.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class CategoryTreeResponse {
    private Long id;
    private String name;
    private String slug;
    private List<CategoryTreeResponse> children;
}

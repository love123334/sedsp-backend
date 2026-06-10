package com.example.secdsp.modules.category.mapper;

import com.example.secdsp.modules.category.dto.request.CreateCategoryRequest;
import com.example.secdsp.modules.category.dto.request.UpdateCategoryRequest;
import com.example.secdsp.modules.category.dto.response.CategoryResponse;
import com.example.secdsp.modules.category.dto.response.CategoryTreeResponse;
import com.example.secdsp.modules.category.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "children", ignore = true)
    Category toEntity(CreateCategoryRequest request);

    @Mapping(target = "parentId", source = "parent.id")
    @Mapping(target = "parentName", source = "parent.name")
    CategoryResponse toResponse(Category category);

    List<CategoryResponse> toResponseList(List<Category> categories);

    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "children", ignore = true)
    void updateEntityFromDto(UpdateCategoryRequest request, @MappingTarget Category category);

    @Mapping(target = "children", expression = "java(mapChildren(category.getChildren()))")
    CategoryTreeResponse toTreeResponse(Category category);

    @Named("mapChildren")
    default List<CategoryTreeResponse> mapChildren(List<Category> children) {
        if (children == null || children.isEmpty()) {
            return null;
        }
        return children.stream()
                .filter(c -> c.getDeletedAt() == null) // Filter active children
                .map(this::toTreeResponse)
                .collect(Collectors.toList());
    }
}

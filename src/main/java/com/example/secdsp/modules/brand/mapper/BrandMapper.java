package com.example.secdsp.modules.brand.mapper;

import com.example.secdsp.modules.brand.dto.request.CreateBrandRequest;
import com.example.secdsp.modules.brand.dto.request.UpdateBrandRequest;
import com.example.secdsp.modules.brand.dto.response.BrandResponse;
import com.example.secdsp.modules.brand.entity.Brand;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BrandMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Brand toEntity(CreateBrandRequest request);

    BrandResponse toResponse(Brand brand);

    List<BrandResponse> toResponseList(List<Brand> brands);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateEntityFromDto(UpdateBrandRequest request, @MappingTarget Brand brand);
}

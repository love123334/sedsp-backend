package com.example.secdsp.modules.product.mapper;

import com.example.secdsp.modules.product.dto.request.*;
import com.example.secdsp.modules.product.dto.response.ProductAttributeResponse;
import com.example.secdsp.modules.product.dto.response.ProductDetailResponse;
import com.example.secdsp.modules.product.dto.response.ProductImageResponse;
import com.example.secdsp.modules.product.dto.response.ProductResponse;
import com.example.secdsp.modules.product.entity.Product;
import com.example.secdsp.modules.product.entity.ProductAttribute;
import com.example.secdsp.modules.product.entity.ProductImage;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "category", ignore = true) // Will be set in service
    @Mapping(target = "seller", ignore = true) // Will be set in service
    @Mapping(target = "productImages", ignore = true) // Handled separately in service
    @Mapping(target = "productAttributes", ignore = true)
        // Handled separately in service
    Product toEntity(CreateProductRequest request);

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "sellerId", source = "seller.id")
    @Mapping(target = "sellerStoreName", expression = "java(resolveStoreName(product))")
    @Mapping(target = "sellerEmail", source = "seller.email")
    @Mapping(target = "sellerPhone", source = "seller.phone")
    @Mapping(target = "primaryImageUrl", expression = "java(resolvePrimaryImageUrl(product))")
    @Mapping(target = "availableQuantity", ignore = true)
    ProductResponse toProductResponse(Product product);

    List<ProductResponse> toProductResponseList(List<Product> products);


    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "sellerId", source = "seller.id")
    @Mapping(target = "sellerStoreName", expression = "java(resolveStoreName(product))")
    @Mapping(target = "images", source = "productImages")
    @Mapping(target = "attributes", source = "productAttributes")
    @Mapping(target = "availableQuantity", ignore = true)
    ProductDetailResponse toProductDetailResponse(Product product);


    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "seller", ignore = true)
    @Mapping(target = "productImages", ignore = true) // Handled separately in service
    @Mapping(target = "productAttributes", ignore = true)
        // Handled separately in service
    void updateProductFromDto(UpdateProductRequest request, @MappingTarget Product product);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "product", ignore = true)
    ProductImage toProductImage(AddProductImageRequest request);

    List<ProductImage> toProductImage(List<AddProductImageRequest> requests);

    @Mapping(target = "isPrimary", source = "primary")
    ProductImageResponse toProductImageResponse(ProductImage productImage);

    List<ProductImageResponse> toProductImageResponseList(List<ProductImage> productImages);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "product", ignore = true)
    ProductAttribute toProductAttribute(AddProductAttributeRequest request);

    List<ProductAttribute> toProductAttribute(List<AddProductAttributeRequest> requests);

    ProductAttributeResponse toProductAttributeResponse(ProductAttribute productAttribute);

    List<ProductAttributeResponse> toProductAttributeResponseList(List<ProductAttribute> productAttributes);

    // Mappings for updating existing images/attributes (if they contain IDs)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true) // ID is for locating, not updating itself from DTO
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "product", ignore = true)
    void updateProductImageFromDto(UpdateProductImageRequest request, @MappingTarget ProductImage productImage);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true) // ID is for locating, not updating itself from DTO
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "product", ignore = true)
    void updateProductAttributeFromDto(UpdateProductAttributeRequest request, @MappingTarget ProductAttribute productAttribute);

    default String resolvePrimaryImageUrl(Product product) {
        if (product == null || product.getProductImages() == null || product.getProductImages().isEmpty()) {
            return null;
        }
        return product.getProductImages().stream()
            .filter(ProductImage::isPrimary)
            .map(ProductImage::getImageUrl)
            .findFirst()
            .orElseGet(() -> product.getProductImages().get(0).getImageUrl());
    }

    default String resolveStoreName(Product product) {
        if (product == null || product.getSeller() == null) {
            return "Cửa hàng SEDSP";
        }
        var seller = product.getSeller();
        if (seller.getStoreName() != null && !seller.getStoreName().isBlank()) {
            return seller.getStoreName().trim();
        }
        if (seller.getFullName() != null && !seller.getFullName().isBlank()) {
            return seller.getFullName().trim();
        }
        return "Cửa hàng SEDSP";
    }
}

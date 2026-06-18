package com.example.secdsp.modules.product.mapper;

import com.example.secdsp.modules.product.dto.request.AddProductAttributeRequest;
import com.example.secdsp.modules.product.dto.request.AddProductImageRequest;
import com.example.secdsp.modules.product.dto.request.CreateProductRequest;
import com.example.secdsp.modules.product.dto.request.UpdateProductRequest;
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
    @Mapping(target = "seller", ignore = true) // Will be set in service
    @Mapping(target = "category", ignore = true) // Will be set in service
    @Mapping(target = "brand", ignore = true) // Will be set in service
    @Mapping(target = "productImages", ignore = true) // Handled separately in service
    @Mapping(target = "productAttributes", ignore = true)
        // Handled separately in service
    Product toEntity(CreateProductRequest request);

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "brandId", source = "brand.id")
    @Mapping(target = "brandName", source = "brand.name")
    @Mapping(target = "sellerId", source = "seller.id")
    @Mapping(target = "sellerStoreName", source = "seller.storeName")
    ProductResponse toProductResponse(Product product);

    List<ProductResponse> toProductResponseList(List<Product> products);


    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "brandId", source = "brand.id")
    @Mapping(target = "brandName", source = "brand.name")
    @Mapping(target = "sellerId", source = "seller.id")
    @Mapping(target = "sellerStoreName", source = "seller.storeName")
    @Mapping(target = "images", source = "productImages")
    @Mapping(target = "attributes", source = "productAttributes")
    ProductDetailResponse toProductDetailResponse(Product product);


    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "seller", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "brand", ignore = true)
    @Mapping(target = "productImages", ignore = true) // Handled separately in service
    @Mapping(target = "productAttributes", ignore = true)
        // Handled separately in service
    void updateProductFromDto(UpdateProductRequest request, @MappingTarget Product product);

    ProductImage toProductImage(AddProductImageRequest request);

    List<ProductImage> toProductImage(List<AddProductImageRequest> requests);

    ProductImageResponse toProductImageResponse(ProductImage productImage);

    List<ProductImageResponse> toProductImageResponseList(List<ProductImage> productImages);

    ProductAttribute toProductAttribute(AddProductAttributeRequest request);

    List<ProductAttribute> toProductAttribute(List<AddProductAttributeRequest> requests);

    ProductAttributeResponse toProductAttributeResponse(ProductAttribute productAttribute);

    List<ProductAttributeResponse> toProductAttributeResponseList(List<ProductAttribute> productAttributes);
}

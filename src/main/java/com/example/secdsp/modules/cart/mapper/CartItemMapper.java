package com.example.secdsp.modules.cart.mapper;

import com.example.secdsp.modules.cart.dto.response.CartItemResponse;
import com.example.secdsp.modules.cart.entity.CartItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CartItemMapper {

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "price", source = "product.price")
    @Mapping(target = "productImageUrl", ignore = true)
    @Mapping(target = "totalPrice", ignore = true)
    CartItemResponse toResponse(CartItem entity);
}
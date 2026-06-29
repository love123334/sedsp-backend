package com.example.secdsp.modules.cart.mapper;

import com.example.secdsp.modules.cart.dto.response.CartResponse;
import com.example.secdsp.modules.cart.entity.Cart;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CartMapper {

    @Mapping(target = "cartId", source = "id")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    CartResponse toResponse(Cart cart);
}
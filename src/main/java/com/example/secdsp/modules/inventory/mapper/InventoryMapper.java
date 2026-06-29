package com.example.secdsp.modules.inventory.mapper;

import com.example.secdsp.modules.inventory.dto.response.InventoryResponse;
import com.example.secdsp.modules.inventory.entity.Inventory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InventoryMapper {

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "currentStock", ignore = true)
    @Mapping(target = "inventoryStatus", ignore = true)
    InventoryResponse toResponse(Inventory inventory);
}
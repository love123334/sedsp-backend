package com.example.secdsp.modules.inventory.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InventoryResponse {

    Long productId;

    String productName;

    Integer availableQuantity;

    Integer reservedQuantity;

    Integer currentStock;

    String inventoryStatus;
}
package com.example.secdsp.modules.inventory.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Inventory information")
public class InventoryResponse {

    @Schema(
        description = "Product identifier",
        example = "101"
    )
    Long productId;

    @Schema(
        description = "Product name",
        example = "Nike Air Force 1"
    )
    String productName;

    @Schema(
        description = "Available quantity ready for sale",
        example = "85"
    )
    Integer availableQuantity;

    @Schema(
        description = "Reserved quantity for pending orders",
        example = "5"
    )
    Integer reservedQuantity;

    @Schema(
        description = "Current stock quantity",
        example = "90"
    )
    Integer currentStock;

    @Schema(
        description = "Current inventory status",
        example = "IN_STOCK"
    )
    String inventoryStatus;
}
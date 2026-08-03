package com.example.secdsp.modules.product.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    description = """
        Product status.

        Available values:
        - ACTIVE : Product is available for sale.
        - INACTIVE : Product is hidden and cannot be purchased.
        - OUT_OF_STOCK : Product is temporarily unavailable due to insufficient inventory.
        """
)
public enum ProductStatus {

    ACTIVE,
    INACTIVE,
    OUT_OF_STOCK
}
package com.example.secdsp.modules.inventory.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Reason for inventory adjustment")
public enum InventoryLogReason {
    MANUAL_ADJUST,
    ORDER,
    ORDER_CANCEL,
    RETURN
}
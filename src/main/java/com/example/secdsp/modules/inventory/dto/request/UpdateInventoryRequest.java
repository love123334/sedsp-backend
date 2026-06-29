package com.example.secdsp.modules.inventory.dto.request;

import com.example.secdsp.modules.inventory.entity.InventoryLogReason;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateInventoryRequest {

    @NotNull(message = "Adjustment quantity is required")
    Integer adjustmentQuantity;

    @NotNull(message = "Adjustment reason is required")
    InventoryLogReason reason;
}
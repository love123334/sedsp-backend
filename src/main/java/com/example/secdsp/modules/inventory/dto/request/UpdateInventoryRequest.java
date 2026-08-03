package com.example.secdsp.modules.inventory.dto.request;

import com.example.secdsp.modules.inventory.entity.InventoryLogReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Inventory update request")
public class UpdateInventoryRequest {

    @Schema(
        description = """
            Quantity adjustment.
            
            Positive values increase stock.
            Negative values decrease stock.
            """,
        example = "20",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Adjustment quantity is required")
    Integer adjustmentQuantity;

    @Schema(
        description = """
            Reason for inventory adjustment.
            
            Available values:
            - MANUAL_ADJUST : Manual stock adjustment
            - ORDER : Stock deducted after order creation
            - ORDER_CANCEL : Stock restored after order cancellation
            - RETURN : Stock restored after product return
            """,
        implementation = InventoryLogReason.class,
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Adjustment reason is required")
    InventoryLogReason reason;
}
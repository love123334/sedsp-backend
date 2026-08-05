package com.example.secdsp.modules.cart.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Request to update the quantity of a cart item")
public class UpdateCartItemRequest {

    @Schema(
        description = "New quantity",
        example = "3",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be greater than 0")
    Integer quantity;
}
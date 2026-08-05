package com.example.secdsp.modules.order.dto.request;

import com.example.secdsp.modules.payment.entity.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Create order request")
public class CreateOrderRequest {

    @Schema(
        description = "Shipping address",
        example = "123 Nguyen Hue Street, District 1, Ho Chi Minh City",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank
    String shippingAddress;

    @Schema(
        description = """
            Payment method.
            
            Available values:
            - MOMO
            - VNPAY
            """,
        implementation = PaymentMethod.class,
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull
    PaymentMethod paymentMethod;
}
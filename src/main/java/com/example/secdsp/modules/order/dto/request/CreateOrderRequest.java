package com.example.secdsp.modules.order.dto.request;

import com.example.secdsp.modules.order.entity.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateOrderRequest {

    @NotBlank
    String shippingAddress;

    @NotNull
    PaymentMethod paymentMethod;
}
package com.example.secdsp.modules.order.dto.request;

import com.example.secdsp.modules.order.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateOrderStatusRequest {

    @NotNull(message = "Status is required")
    OrderStatus status;

    String note;
}

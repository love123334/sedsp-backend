package com.example.secdsp.modules.order.mapper;

import com.example.secdsp.modules.order.dto.response.PaymentResponse;
import com.example.secdsp.modules.order.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(target = "orderId", source = "order.id")
    PaymentResponse toResponse(Payment payment);
}

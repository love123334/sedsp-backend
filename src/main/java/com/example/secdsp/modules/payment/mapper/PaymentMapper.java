package com.example.secdsp.modules.payment.mapper;

import com.example.secdsp.modules.payment.dto.response.PaymentResponse;
import com.example.secdsp.modules.payment.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(target = "orderId", source = "order.id")
    PaymentResponse toResponse(Payment payment);
}

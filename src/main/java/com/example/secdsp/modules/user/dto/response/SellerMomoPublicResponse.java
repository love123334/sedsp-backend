package com.example.secdsp.modules.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Public seller MoMo info for checkout")
public class SellerMomoPublicResponse {

    Long sellerId;

    String storeName;

    String momoPhone;

    String momoQrUrl;

    boolean configured;
}

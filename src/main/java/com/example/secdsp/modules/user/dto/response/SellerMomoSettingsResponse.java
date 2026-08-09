package com.example.secdsp.modules.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Seller MoMo payment settings")
public class SellerMomoSettingsResponse {

    @Schema(description = "MoMo phone configured for receiving transfers")
    String momoPhone;

    @Schema(description = "MoMo QR image URL")
    String momoQrUrl;

    @Schema(description = "True when phone or QR is set")
    boolean configured;
}

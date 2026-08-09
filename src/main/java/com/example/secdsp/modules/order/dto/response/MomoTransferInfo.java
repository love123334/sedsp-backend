package com.example.secdsp.modules.order.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "MoMo transfer instructions for seller QR checkout")
public class MomoTransferInfo {

    @Schema(description = "Amount to transfer in VND", example = "1245000")
    BigDecimal amount;

    @Schema(description = "Transfer note / content", example = "SEDSP DH#12847")
    String transferNote;

    @Schema(description = "Seller MoMo phone", example = "0901234567")
    String sellerMomoPhone;

    @Schema(description = "Seller uploaded MoMo QR image URL")
    String sellerMomoQrUrl;

    @Schema(description = "Seller store display name")
    String sellerStoreName;

    @Schema(description = "Whether seller has configured MoMo payment")
    boolean configured;
}

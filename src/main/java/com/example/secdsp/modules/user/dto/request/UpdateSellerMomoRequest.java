package com.example.secdsp.modules.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Seller MoMo QR / phone settings for manual transfer checkout")
@Getter
@Setter
@NoArgsConstructor
public class UpdateSellerMomoRequest {

    @Schema(description = "MoMo wallet phone (VN)", example = "0901234567")
    @Size(max = 20)
    @Pattern(regexp = "^(|[0-9+\\-\\s]{8,20})$", message = "Invalid phone format")
    private String momoPhone;

    @Schema(description = "Public URL of seller MoMo QR image")
    @Size(max = 2048)
    private String momoQrUrl;
}

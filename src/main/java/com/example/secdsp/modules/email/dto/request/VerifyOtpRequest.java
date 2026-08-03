package com.example.secdsp.modules.email.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "OTP verification request")
public class VerifyOtpRequest {

    @Schema(
        description = "Email address",
        example = "user@example.com",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String email;

    @Schema(
        description = "One-time password",
        example = "123456",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String otp;
}
package com.example.secdsp.modules.email.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Update password request")
public class UpdatePasswordRequest {

    @Schema(
        description = "Email address",
        example = "user@example.com",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String email;

    @Schema(
        description = "New password",
        example = "Password@123",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String newPassword;

    @Schema(
        description = "Confirm new password",
        example = "Password@123",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String confirmPassword;
}
package com.example.secdsp.modules.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "User registration request")
public class RegisterRequest {

    @Schema(
        description = "Full name",
        example = "Nguyen Van A",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank
    @Size(max = 150)
    String fullName;

    @Schema(
        description = "Email address",
        example = "user@example.com",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank
    @Email
    @Size(max = 150)
    String email;

    @Schema(
        description = "Password",
        example = "Password@123",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank
    @Size(min = 8)
    String password;

    @Schema(
        description = "Confirm password",
        example = "Password@123",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank
    String confirmPassword;
}
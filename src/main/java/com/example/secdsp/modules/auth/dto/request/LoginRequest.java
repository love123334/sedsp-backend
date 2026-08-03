package com.example.secdsp.modules.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Login request")
public class LoginRequest {

    @Schema(
        description = "User email",
        example = "admin@example.com",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank
    @Email
    String email;

    @Schema(
        description = "User password",
        example = "Password@123",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank
    @Size(min = 8, max = 72)
    String password;
}

package com.example.secdsp.modules.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Login response")
public class LoginResponse {

    @Schema(
        description = "Token type",
        example = "Bearer"
    )
    String tokenType;

    @Schema(
        description = "JWT access token"
    )
    String accessToken;

    @Schema(
        description = "Access token expiration time in seconds",
        example = "3600"
    )
    long expiresInSeconds;

    @Schema(description = "Authenticated user")
    CurrentUserSummary user;
}

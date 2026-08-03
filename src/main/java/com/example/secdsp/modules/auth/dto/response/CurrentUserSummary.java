package com.example.secdsp.modules.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Authenticated user summary")
public class CurrentUserSummary {

    @Schema(
        description = "User identifier",
        example = "1"
    )
    Long id;

    @Schema(
        description = "Email address",
        example = "admin@example.com"
    )
    String email;

    @Schema(
        description = "Username",
        example = "admin"
    )
    String username;

    @Schema(
        description = "User role",
        example = "ADMIN"
    )
    String role;
}
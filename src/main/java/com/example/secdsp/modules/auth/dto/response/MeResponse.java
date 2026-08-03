package com.example.secdsp.modules.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Current authenticated user")
public class MeResponse {

    @Schema(example = "1")
    Long id;

    @Schema(example = "admin@example.com")
    String email;

    @Schema(example = "admin")
    String username;

    @Schema(example = "Nguyen Van A")
    String fullName;

    @Schema(example = "0912345678")
    String phone;

    @Schema(example = "ADMIN")
    String role;
}

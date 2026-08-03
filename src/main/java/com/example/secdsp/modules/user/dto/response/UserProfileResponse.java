package com.example.secdsp.modules.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Schema(
    description = "Detailed user profile information."
)
@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserProfileResponse {

    @Schema(description = "User ID.", example = "1")
    Long id;

    @Schema(description = "Username.", example = "johnsmith")
    String username;

    @Schema(description = "Email address.", example = "john@example.com")
    String email;

    @Schema(description = "Full name.", example = "John Smith")
    String fullName;

    @Schema(description = "User role.", example = "CUSTOMER")
    String role;

    @Schema(description = "Account status.", example = "ACTIVE")
    String status;
}

package com.example.secdsp.modules.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(
    description = "Request for updating the authenticated user's profile."
)
@Getter
@Setter
@NoArgsConstructor
public class UpdateProfileRequest {

    @Schema(
        description = "User's full name.",
        example = "John Smith"
    )
    @Size(max = 150)
    private String fullName;

    @Schema(
        description = "User's phone number.",
        example = "0912345678"
    )
    @Size(max = 20)
    private String phone;
}
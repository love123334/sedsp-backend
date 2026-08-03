package com.example.secdsp.modules.user.dto.request;

import com.example.secdsp.modules.user.entity.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Schema(
    description = "Request for assigning a role to a user."
)
@Getter
@Setter
public class AssignRoleRequest {

    @Schema(
        description = """
            Available values:
            - CUSTOMER : Customer account.
            - SELLER : Seller account.
            - MANAGER : Manager account.
            - ADMIN : Administrator account.
            """,
        implementation = UserRole.class
    )
    @NotNull(message = "Role is required")
    private UserRole role;

}
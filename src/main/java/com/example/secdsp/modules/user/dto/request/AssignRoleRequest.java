package com.example.secdsp.modules.user.dto.request;

import com.example.secdsp.modules.user.entity.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignRoleRequest {

    @NotNull(message = "Role is required")
    private UserRole role;

}
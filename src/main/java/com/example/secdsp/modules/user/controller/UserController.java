package com.example.secdsp.modules.user.controller;

import com.example.secdsp.common.api.BaseResponse;
import com.example.secdsp.modules.user.dto.request.AssignRoleRequest;
import com.example.secdsp.modules.user.dto.request.UpdateProfileRequest;
import com.example.secdsp.modules.user.dto.response.UserProfileResponse;
import com.example.secdsp.modules.user.dto.response.UserSummaryResponse;
import com.example.secdsp.modules.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(
    name = "Users",
    description = "APIs for managing user accounts and profiles."
)
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "Get current user profile",
        description = "Retrieve the profile information of the authenticated user."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    })
    @GetMapping("/profile")
    public ResponseEntity<BaseResponse<UserProfileResponse>> getProfile() {
        UserProfileResponse response = userService.getProfile();
        return ResponseEntity.ok(BaseResponse.success("Profile retrieved successfully", response));
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "Update current user profile",
        description = "Update the profile information of the authenticated user."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid profile information", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    })
    @PutMapping("/profile")
    public ResponseEntity<BaseResponse<UserProfileResponse>> updateProfile(
        @Valid @RequestBody UpdateProfileRequest request
    ) {
        UserProfileResponse response = userService.updateProfile(request);
        return ResponseEntity.ok(BaseResponse.success("Profile updated successfully", response));
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "Get users",
        description = "Retrieve a paginated list of users."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Access denied", content = @Content)
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<Page<UserSummaryResponse>>> getUsers(
        @Parameter(
            description = "Keyword for searching users by username, email or full name.",
            example = "john"
        )
        @RequestParam(required = false) String keyword,

        @ParameterObject
        @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<UserSummaryResponse> response = userService.getUsers(keyword, pageable);
        return ResponseEntity.ok(BaseResponse.success("Users retrieved successfully", response));
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "Get user by ID",
        description = "Retrieve detailed information of a specific user."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Access denied", content = @Content),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<UserProfileResponse>> getUserById(
        @Parameter(description = "User ID.", example = "1")
        @PathVariable Long id
    ) {
        UserProfileResponse response = userService.getUserById(id);
        return ResponseEntity.ok(BaseResponse.success("User retrieved successfully", response));
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "Assign user role",
        description = "Assign a new role to a specific user."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Role updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid role", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Access denied", content = @Content),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<Void>> assignRole(
        @Parameter(description = "User ID.", example = "1")
        @PathVariable Long id,
        @Valid @RequestBody AssignRoleRequest request
    ) {

        userService.assignRole(id, request);

        return ResponseEntity.ok(
            BaseResponse.success("Role updated successfully")
        );
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "Activate user",
        description = "Activate a user account."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User activated successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Access denied", content = @Content),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<Void>> activateUser(
        @Parameter(description = "User ID.", example = "1")
        @PathVariable Long id
    ) {

        userService.activateUser(id);

        return ResponseEntity.ok(
            BaseResponse.success("User activated successfully")
        );
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "Deactivate user",
        description = "Deactivate a user account."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User deactivated successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Access denied", content = @Content),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<Void>> deactivateUser(
        @Parameter(description = "User ID.", example = "1")
        @PathVariable Long id
    ) {

        userService.deactivateUser(id);

        return ResponseEntity.ok(
            BaseResponse.success("User deactivated successfully")
        );
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "Delete user",
        description = "Soft-delete a user account (admin only)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User deleted successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid delete request", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Access denied", content = @Content),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<Void>> deleteUser(
        @Parameter(description = "User ID.", example = "1")
        @PathVariable Long id
    ) {
        userService.deleteUser(id);
        return ResponseEntity.ok(BaseResponse.success("User deleted successfully"));
    }
}

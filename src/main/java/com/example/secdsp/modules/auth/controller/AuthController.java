package com.example.secdsp.modules.auth.controller;

import com.example.secdsp.common.api.BaseResponse;
import com.example.secdsp.modules.auth.dto.request.LoginRequest;
import com.example.secdsp.modules.auth.dto.request.RegisterRequest;
import com.example.secdsp.modules.auth.dto.response.LoginResponse;
import com.example.secdsp.modules.auth.dto.response.MeResponse;
import com.example.secdsp.modules.auth.service.AuthService;
import com.example.secdsp.modules.email.dto.request.UpdatePasswordRequest;
import com.example.secdsp.modules.email.dto.request.VerifyOtpRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(
    name = "Authentication",
    description = "Authentication and account management APIs"
)
public class AuthController {

    private final AuthService authService;

    @Operation(
        summary = "User login",
        description = "Authenticate a user using email and password, then return a JWT access token."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful"),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
        @ApiResponse(responseCode = "401", description = "Invalid email or password", content = @Content)
    })
    @PostMapping("/login")
    public ResponseEntity<BaseResponse<LoginResponse>> login(
        @Valid @RequestBody LoginRequest request
    ) {

        LoginResponse response = authService.login(request);

        return ResponseEntity.ok(BaseResponse.success("Login successful", response));
    }

    @Operation(
        summary = "Current user",
        description = "Retrieve information about the currently authenticated user."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Current user retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    })
    @GetMapping("/me")
    public ResponseEntity<BaseResponse<MeResponse>> getCurrentUser() {

        MeResponse response = authService.getCurrentUser();

        return ResponseEntity.ok(BaseResponse.success("Current user retrieved successfully", response));
    }

    @Operation(
        summary = "Register account",
        description = "Create a new user account. An OTP will be sent to verify the email address."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Registration successful"),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
        @ApiResponse(responseCode = "409", description = "Email already exists", content = @Content)
    })
    @PostMapping("/register")
    public ResponseEntity<BaseResponse<Void>> register(
        @Valid @RequestBody RegisterRequest request
    ) {

        authService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(BaseResponse.success("Registration successful"));
    }

    @Operation(
        summary = "Resend OTP",
        description = "Resend the registration verification OTP to the specified email address."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OTP resent successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid email", content = @Content)
    })
    @PostMapping("/resend-otp")
    public ResponseEntity<BaseResponse<Void>> resendOtp(
        @Parameter(
            description = "Registered email address",
            example = "john@example.com"
        )
        @RequestParam String email
    ) {

        authService.resendOtp(email);

        return ResponseEntity.ok(BaseResponse.success("OTP resent successfully"));
    }

    @Operation(
        summary = "Forgot password",
        description = "Send a password reset OTP to the specified email if the account exists."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OTP sent if email exists")
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<BaseResponse<Void>> forgotPassword(
        @Parameter(
            description = "Account email",
            example = "john@example.com"
        )
        @RequestParam String email
    ) {

        authService.forgotPassword(email);

        return ResponseEntity.ok(BaseResponse.success("OTP sent if email exists"));
    }

    @Operation(
        summary = "Verify password reset OTP",
        description = "Verify the OTP before allowing the user to update the password."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OTP verified successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid or expired OTP", content = @Content)
    })
    @PostMapping("/verify-reset-otp")
    public ResponseEntity<BaseResponse<Void>> verifyResetOtp(
        @Valid @RequestBody VerifyOtpRequest request
    ) {

        authService.verifyResetOtp(request);

        return ResponseEntity.ok(BaseResponse.success("OTP verified successfully"));
    }

    @Operation(
        summary = "Update password",
        description = "Update the account password after successful OTP verification."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Password updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content)
    })
    @PostMapping("/update-password")
    public ResponseEntity<BaseResponse<Void>> updatePassword(
        @Valid @RequestBody UpdatePasswordRequest request
    ) {

        authService.updatePassword(request);

        return ResponseEntity.ok(BaseResponse.success("Password updated successfully"));
    }

}
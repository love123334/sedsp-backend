package com.example.secdsp.modules.auth.controller;

import com.example.secdsp.common.api.ApiResponse;
import com.example.secdsp.modules.auth.dto.request.LoginRequest;
import com.example.secdsp.modules.auth.dto.request.RegisterRequest;
import com.example.secdsp.modules.auth.dto.response.LoginResponse;
import com.example.secdsp.modules.auth.dto.response.MeResponse;
import com.example.secdsp.modules.auth.service.AuthService;
import com.example.secdsp.modules.email.dto.request.UpdatePasswordRequest;
import com.example.secdsp.modules.email.dto.request.VerifyOtpRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MeResponse>> getCurrentUser() {
        MeResponse response = authService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success("Current user retrieved successfully", response));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(
        @Valid @RequestBody RegisterRequest request
    ) {

        authService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(
                "Registration successful"
            ));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<Void>> resendOtp(
        @RequestParam String email) {

        authService.resendOtp(email);

        return ResponseEntity.ok(
            ApiResponse.success("OTP resent successfully"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
        @RequestParam String email) {

        authService.forgotPassword(email);

        return ResponseEntity.ok(
            ApiResponse.success("OTP sent if email exists")
        );
    }

    @PostMapping("/verify-reset-otp")
    public ResponseEntity<ApiResponse<Void>> verifyResetOtp(
        @RequestBody VerifyOtpRequest request) {

        authService.verifyResetOtp(request);

        return ResponseEntity.ok(
            ApiResponse.success("OTP verified successfully")
        );
    }

    @PostMapping("/update-password")
    public ResponseEntity<ApiResponse<Void>> updatePassword(
        @RequestBody UpdatePasswordRequest request) {

        authService.updatePassword(request);

        return ResponseEntity.ok(
            ApiResponse.success("Password updated successfully")
        );
    }
}

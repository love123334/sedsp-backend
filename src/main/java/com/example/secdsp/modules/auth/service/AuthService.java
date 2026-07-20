package com.example.secdsp.modules.auth.service;

import com.example.secdsp.modules.auth.dto.request.LoginRequest;
import com.example.secdsp.modules.auth.dto.request.RegisterRequest;
import com.example.secdsp.modules.auth.dto.response.LoginResponse;
import com.example.secdsp.modules.auth.dto.response.MeResponse;
import com.example.secdsp.modules.email.dto.request.UpdatePasswordRequest;
import com.example.secdsp.modules.email.dto.request.VerifyOtpRequest;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    MeResponse getCurrentUser();

    void register(RegisterRequest request);

    void resendOtp(String email);

    void forgotPassword(String email);

    void verifyResetOtp(VerifyOtpRequest request);

    void updatePassword(UpdatePasswordRequest request);
}

package com.example.secdsp.modules.email.service;

public interface OtpService {

    String generateOtp(String email);

    void validateOtp(String email, String otp);

    String resendOtp(String email);
}

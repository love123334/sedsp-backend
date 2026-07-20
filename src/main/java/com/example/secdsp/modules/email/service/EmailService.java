package com.example.secdsp.modules.email.service;

public interface EmailService {

    void sendOtp(String toEmail, String otp);

    void sendResetPasswordOtp(String toEmail, String otp);

}
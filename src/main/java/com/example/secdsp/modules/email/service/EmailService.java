package com.example.secdsp.modules.email.service;

public interface EmailService {

    void sendOtp(String toEmail, String otp);

    void sendResetPasswordOtp(String toEmail, String otp);

    /** Thông báo vòng đời đơn — buyer / seller */
    void sendOrderLifecycleEmail(
        String toEmail,
        String recipientName,
        String roleLabel,
        Long orderId,
        String statusLabel,
        String detailHtml
    );
}

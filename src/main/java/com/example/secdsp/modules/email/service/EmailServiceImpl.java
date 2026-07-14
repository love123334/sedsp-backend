package com.example.secdsp.modules.email.service;

import com.example.secdsp.config.MailProperties;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;

    @Override
    public void sendOtp(String toEmail, String otp) {

        sendHtmlEmail(
            toEmail,
            "🔐 Email Verification - SECDSP",
            buildOtpTemplate(otp)
        );
    }

    @Override
    public void sendResetPasswordOtp(String toEmail, String otp) {

        sendHtmlEmail(
            toEmail,
            "🔑 Reset Password - SECDSP",
            buildResetPasswordTemplate(otp)
        );
    }

    private void sendHtmlEmail(String toEmail, String subject, String content) {

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailProperties.getFrom());
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(content, true);

            ClassPathResource logo =
                new ClassPathResource(mailProperties.getLogoPath());

            helper.addInline("logoImage", logo);

            mailSender.send(message);

            log.info("Email sent successfully to {}", toEmail);

        } catch (Exception e) {
            log.error("Error sending email to {}", toEmail, e);
            throw new RuntimeException("Unable to send email at the moment");
        }
    }

    private String buildOtpTemplate(String otp) {

        return """
                <div style="font-family: Arial, sans-serif; background:#f4f6f8; padding:40px;">
                
                    <div style="max-width:520px; margin:auto; background:white; 
                                padding:35px; border-radius:12px; text-align:center;
                                box-shadow:0 5px 15px rgba(0,0,0,0.08);">
                        
                        <img src='cid:logoImage' width="130" style="margin-bottom:25px"/>
                        
                        <h2 style="color:#2c3e50; margin-bottom:10px;">
                            Email Verification
                        </h2>
                        
                        <p style="color:#555; font-size:15px;">
                            Thank you for registering with <strong>SECDSP</strong>.
                        </p>
                        
                        <p style="color:#555; font-size:15px;">
                            Please enter the OTP below to verify your account:
                        </p>
                        
                        <div style="
                            font-size:32px;
                            font-weight:bold;
                            letter-spacing:8px;
                            background:#f1f3f5;
                            padding:18px;
                            margin:25px 0;
                            border-radius:10px;
                            color:#34495e;">
                            %s
                        </div>
                        
                        <p style="color:#888; font-size:14px;">
                            This OTP will expire in %d minutes.
                        </p>
                        
                        <hr style="margin:25px 0; border:none; border-top:1px solid #eee"/>
                        
                        <p style="font-size:12px; color:#aaa;">
                            If you did not request this email, please ignore it.
                        </p>
                        
                    </div>
                
                </div>
                """
            .formatted(otp, mailProperties.getOtpExpirationMinutes());
    }

    private String buildResetPasswordTemplate(String otp) {

        return """
                <div style="font-family: Arial, sans-serif; background:#f4f6f8; padding:40px;">
                
                    <div style="max-width:520px; margin:auto; background:white; 
                                padding:35px; border-radius:12px; text-align:center;
                                box-shadow:0 5px 15px rgba(0,0,0,0.08);">
                        
                        <img src='cid:logoImage' width="130" style="margin-bottom:25px"/>
                        
                        <h2 style="color:#e74c3c; margin-bottom:10px;">
                            Reset Password
                        </h2>
                        
                        <p style="color:#555; font-size:15px;">
                            We received a request to reset your password.
                        </p>
                        
                        <div style="
                            font-size:32px;
                            font-weight:bold;
                            letter-spacing:8px;
                            background:#f1f3f5;
                            padding:18px;
                            margin:25px 0;
                            border-radius:10px;
                            color:#e74c3c;">
                            %s
                        </div>
                        
                        <p style="color:#888; font-size:14px;">
                            This OTP will expire in %d minutes.
                        </p>
                        
                        <hr style="margin:25px 0; border:none; border-top:1px solid #eee"/>
                        
                        <p style="font-size:12px; color:#aaa;">
                            If you did not request this, please secure your account.
                        </p>
                        
                    </div>
                
                </div>
                """
            .formatted(otp, mailProperties.getOtpExpirationMinutes());
    }
}
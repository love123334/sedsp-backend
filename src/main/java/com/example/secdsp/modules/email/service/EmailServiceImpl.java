package com.example.secdsp.modules.email.service;

import com.example.secdsp.common.exception.EmailServiceException;
import com.example.secdsp.config.MailProperties;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private static final String RESEND_TEST_FROM = "SEDSP <onboarding@resend.dev>";

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;
    private final RestTemplate restTemplate;

    @Override
    public void sendOtp(String toEmail, String otp) {
        sendHtmlEmail(
            toEmail,
            "SEDSP - Ma xac thuc dang ky (OTP)",
            buildOtpTemplate(otp)
        );
    }

    @Override
    public void sendResetPasswordOtp(String toEmail, String otp) {
        sendHtmlEmail(
            toEmail,
            "SEDSP - Ma dat lai mat khau (OTP)",
            buildResetPasswordTemplate(otp)
        );
    }

    @Override
    public void sendOrderLifecycleEmail(
        String toEmail,
        String recipientName,
        String roleLabel,
        Long orderId,
        String statusLabel,
        String detailHtml
    ) {
        String subject = "SEDSP - Đơn #" + orderId + " - " + statusLabel;
        String html = """
            <div style="font-family:Arial,sans-serif;background:#f4f6f8;padding:32px;">
              <div style="max-width:560px;margin:auto;background:#fff;padding:28px;border-radius:12px;">
                <img src='cid:logoImage' width="120" style="margin-bottom:18px"/>
                <h2 style="color:#0f172a;margin:0 0 12px;">Cập nhật đơn hàng #%s</h2>
                <p style="color:#475569;">Xin chào <strong>%s</strong> (%s),</p>
                <p style="color:#334155;">Trạng thái: <strong style="color:#0d9488;">%s</strong></p>
                <div style="margin:16px 0;padding:14px;background:#f8fafc;border-radius:8px;color:#334155;font-size:14px;">
                  %s
                </div>
                <p style="font-size:12px;color:#94a3b8;">Đây là email tự động từ hệ thống SEDSP.</p>
              </div>
            </div>
            """.formatted(
            orderId,
            escape(recipientName),
            escape(roleLabel),
            escape(statusLabel),
            detailHtml == null ? "" : detailHtml
        );
        sendHtmlEmail(toEmail, subject, html);
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void sendHtmlEmail(String toEmail, String subject, String content) {
        if (isResendConfigured()) {
            sendViaResend(toEmail, subject, content);
            return;
        }
        sendViaSmtp(toEmail, subject, content);
    }

    private boolean isResendConfigured() {
        String key = mailProperties.getResendApiKey();
        return key != null && !key.isBlank();
    }

    /**
     * HTTPS API — works on Railway Hobby where outbound SMTP (25/465/587) is blocked.
     */
    private void sendViaResend(String toEmail, String subject, String content) {
        String from = resolveResendFrom();
        String html = replaceCidLogoWithText(content);

        Map<String, Object> body = new HashMap<>();
        body.put("from", from);
        body.put("to", List.of(toEmail));
        body.put("subject", subject);
        body.put("html", html);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(mailProperties.getResendApiKey().trim());
        // Resend rejects requests without User-Agent (403 / error 1010)
        headers.set(HttpHeaders.USER_AGENT, "sedsp-backend/1.0");

        String url = mailProperties.getResendApiUrl() == null || mailProperties.getResendApiUrl().isBlank()
            ? "https://api.resend.com/emails"
            : mailProperties.getResendApiUrl().trim();

        try {
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> res = restTemplate.postForEntity(
                url,
                new HttpEntity<>(body, headers),
                Map.class
            );
            if (!res.getStatusCode().is2xxSuccessful()) {
                throw new EmailServiceException("Resend HTTP " + res.getStatusCode().value()
                    + (res.getBody() != null ? ": " + res.getBody() : ""));
            }
            Object id = res.getBody() != null ? res.getBody().get("id") : null;
            log.info("Email sent via Resend to {} id={}", toEmail, id);
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            log.error("Resend API HTTP {} for {}: {}", e.getStatusCode().value(), toEmail, e.getResponseBodyAsString());
            throw new EmailServiceException(
                "Unable to send email via Resend (" + e.getStatusCode().value() + "): "
                    + e.getResponseBodyAsString()
            );
        } catch (RestClientException e) {
            log.error("Resend API failed for {}", toEmail, e);
            throw new EmailServiceException("Unable to send email via Resend: " + e.getMessage());
        }
    }

    private String resolveResendFrom() {
        String from = mailProperties.getFrom();
        if (from != null && !from.isBlank() && !from.startsWith("your_email")) {
            return from.trim();
        }
        // Resend test sender — only delivers to the Resend account owner until a domain is verified
        return RESEND_TEST_FROM;
    }

    private void sendViaSmtp(String toEmail, String subject, String content) {
        String username = System.getenv("MAIL_USERNAME");
        String from = mailProperties.getFrom();
        if ((from == null || from.isBlank() || from.startsWith("your_email"))
            && (username == null || username.isBlank())) {
            throw new EmailServiceException(
                "Mail chua cau hinh. Dat RESEND_API_KEY (+ MAIL_FROM) tren Railway, "
                    + "hoac MAIL_USERNAME / MAIL_PASSWORD / MAIL_FROM (SMTP — can Pro)."
            );
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            ClassPathResource logo = new ClassPathResource(mailProperties.getLogoPath());
            boolean hasLogo = logo.exists();

            MimeMessageHelper helper =
                new MimeMessageHelper(message, hasLogo, "UTF-8");

            String fromAddr = (from != null && !from.isBlank() && !from.startsWith("your_email"))
                ? from.trim()
                : username.trim();
            helper.setFrom(fromAddr);
            helper.setTo(toEmail);
            helper.setSubject(subject);

            String html = hasLogo ? content : replaceCidLogoWithText(content);
            helper.setText(html, true);

            if (hasLogo) {
                helper.addInline("logoImage", logo);
            }

            mailSender.send(message);
            log.info("Email sent via SMTP to {}", toEmail);
        } catch (Exception e) {
            log.error("Error sending email to {}", toEmail, e);
            throw new EmailServiceException("Unable to send email at the moment: " + e.getMessage());
        }
    }

    private static String replaceCidLogoWithText(String content) {
        return content
            .replace(
                "<img src='cid:logoImage' width=\"120\" style=\"margin-bottom:18px\"/>",
                "<div style=\"font-size:20px;font-weight:700;color:#0f172a;margin-bottom:16px\">SEDSP</div>"
            )
            .replace(
                "<img src='cid:logoImage' width=\"130\" style=\"margin-bottom:25px\"/>",
                "<div style=\"font-size:22px;font-weight:700;color:#0f172a;margin-bottom:20px\">SEDSP</div>"
            );
    }

    private String buildOtpTemplate(String otp) {
        return """
                <div style="font-family: Arial, sans-serif; background:#f4f6f8; padding:40px;">
                    <div style="max-width:520px; margin:auto; background:white;
                                padding:35px; border-radius:12px; text-align:center;
                                box-shadow:0 5px 15px rgba(0,0,0,0.08);">
                        <img src='cid:logoImage' width="130" style="margin-bottom:25px"/>
                        <h2 style="color:#2c3e50; margin-bottom:10px;">Xac thuc email dang ky</h2>
                        <p style="color:#555; font-size:15px;">Cam on ban da dang ky <strong>SEDSP</strong>.</p>
                        <p style="color:#555; font-size:15px;">Nhap ma OTP ben duoi de kich hoat tai khoan:</p>
                        <div style="font-size:32px;font-weight:bold;letter-spacing:8px;background:#f1f3f5;
                            padding:18px;margin:25px 0;border-radius:10px;color:#34495e;">%s</div>
                        <p style="color:#888; font-size:14px;">Ma OTP het han sau %d phut.</p>
                    </div>
                </div>
                """
            .formatted(otp, mailProperties.getOtpExpirationMinutes());
    }

    private String buildResetPasswordTemplate(String otp) {
        return """
                <div style="font-family: Arial, sans-serif; background:#f4f6f8; padding:40px;">
                    <div style="max-width:520px; margin:auto; background:white;
                                padding:35px; border-radius:12px; text-align:center;">
                        <img src='cid:logoImage' width="130" style="margin-bottom:25px"/>
                        <h2 style="color:#2c3e50;">Dat lai mat khau</h2>
                        <p style="color:#555;">Ma OTP dat lai mat khau cua ban:</p>
                        <div style="font-size:32px;font-weight:bold;letter-spacing:8px;
                            background:#f1f3f5;padding:18px;margin:25px 0;border-radius:10px;">%s</div>
                        <p style="color:#888; font-size:14px;">Het han sau %d phut.</p>
                    </div>
                </div>
                """
            .formatted(otp, mailProperties.getOtpExpirationMinutes());
    }
}

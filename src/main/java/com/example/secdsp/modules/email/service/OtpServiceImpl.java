package com.example.secdsp.modules.email.service;

import com.example.secdsp.common.exception.BusinessException;
import com.example.secdsp.config.MailProperties;
import com.example.secdsp.modules.email.entity.EmailOtp;
import com.example.secdsp.modules.email.repository.EmailOtpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private final EmailOtpRepository emailOtpRepository;
    private final MailProperties mailProperties;

    @Override
    public String generateOtp(String email) {

        String otp = generateRandomOtp();

        EmailOtp emailOtp = new EmailOtp();
        emailOtp.setEmail(email);
        emailOtp.setOtp(otp);
        emailOtp.setExpiryTime(
            LocalDateTime.now()
                .plusMinutes(mailProperties.getOtpExpirationMinutes()));
        emailOtp.setUsed(false);
        emailOtp.setCreatedAt(LocalDateTime.now());
        emailOtp.setResendCount(0);

        emailOtpRepository.save(emailOtp);

        return otp;
    }

    @Override
    public String resendOtp(String email) {

        EmailOtp latestOtp = emailOtpRepository
            .findTopByEmailOrderByIdDesc(email)
            .orElseThrow(() ->
                             new BusinessException("OTP not found", HttpStatus.BAD_REQUEST));

        if (latestOtp.getCreatedAt()
            .plusSeconds(mailProperties.getResendCooldownSeconds())
            .isAfter(LocalDateTime.now())) {

            throw new BusinessException(
                "Please wait before requesting another OTP",
                HttpStatus.TOO_MANY_REQUESTS
            );
        }

       if (latestOtp.getResendCount() >= mailProperties.getMaxResendAttempts()) {
            throw new BusinessException(
                "Maximum resend attempts exceeded",
                HttpStatus.TOO_MANY_REQUESTS
            );
        }

        // Generate new OTP
        String newOtp = generateRandomOtp();

        latestOtp.setOtp(newOtp);
        latestOtp.setExpiryTime(
            LocalDateTime.now()
                .plusMinutes(mailProperties.getOtpExpirationMinutes()));
        latestOtp.setCreatedAt(LocalDateTime.now());
        latestOtp.setResendCount(latestOtp.getResendCount() + 1);
        latestOtp.setUsed(false);
        latestOtp.setVerified(false);

        emailOtpRepository.save(latestOtp);

        return newOtp;
    }

    @Override
    public void validateOtp(String email, String otp) {

        EmailOtp emailOtp = emailOtpRepository
            .findTopByEmailOrderByIdDesc(email)
            .orElseThrow(() ->
                             new BusinessException("OTP not found", HttpStatus.BAD_REQUEST));

        if (emailOtp.isUsed()) {
            throw new BusinessException("OTP already used", HttpStatus.BAD_REQUEST);
        }

        if (emailOtp.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("OTP expired", HttpStatus.BAD_REQUEST);
        }

        if (!emailOtp.getOtp().equals(otp)) {
            throw new BusinessException("Invalid OTP", HttpStatus.BAD_REQUEST);
        }

        emailOtp.setUsed(true);
        emailOtp.setVerified(true);
        emailOtpRepository.save(emailOtp);
    }

    private String generateRandomOtp() {
        return String.valueOf(
            ThreadLocalRandom.current().nextInt(100000, 999999));
    }
}
package com.example.secdsp.modules.auth.service;

import com.example.secdsp.common.exception.BusinessException;
import com.example.secdsp.common.exception.ResourceNotFoundException;
import com.example.secdsp.common.exception.UnauthorizedException;
import com.example.secdsp.common.util.SecurityUtils;
import com.example.secdsp.modules.auth.dto.request.LoginRequest;
import com.example.secdsp.modules.auth.dto.request.RegisterRequest;
import com.example.secdsp.modules.auth.dto.response.LoginResponse;
import com.example.secdsp.modules.auth.dto.response.MeResponse;
import com.example.secdsp.modules.auth.mapper.AuthMapper;
import com.example.secdsp.modules.email.dto.request.UpdatePasswordRequest;
import com.example.secdsp.modules.email.dto.request.VerifyOtpRequest;
import com.example.secdsp.modules.email.entity.EmailOtp;
import com.example.secdsp.modules.email.repository.EmailOtpRepository;
import com.example.secdsp.modules.email.service.EmailService;
import com.example.secdsp.modules.email.service.OtpService;
import com.example.secdsp.modules.user.entity.Role;
import com.example.secdsp.modules.user.entity.User;
import com.example.secdsp.modules.user.entity.UserRole;
import com.example.secdsp.modules.user.entity.UserStatus;
import com.example.secdsp.modules.user.repository.RoleRepository;
import com.example.secdsp.modules.user.repository.UserRepository;
import com.example.secdsp.security.jwt.JwtProvider;
import com.example.secdsp.security.user.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final AuthMapper authMapper;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final EmailService emailService;
    private final PlatformTransactionManager transactionManager;
    private final EmailOtpRepository emailOtpRepository;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getEmail(),
                    request.getPassword()
                )
            );

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

            User user = userRepository.findById(userDetails.getId())
                .orElseThrow();

            if (user.getStatus() == UserStatus.PENDING) {
                throw new UnauthorizedException("Please verify your email first");
            }

            String accessToken = jwtProvider.generateAccessToken(userDetails.getId());

            long expiresInSeconds = jwtExpirationMs / 1000;

            return authMapper.toLoginResponse(
                accessToken,
                expiresInSeconds,
                userDetails.getId(),
                userDetails.getEmail(),
                userDetails.getRole()
            );

        } catch (BadCredentialsException ex) {
            throw new UnauthorizedException("Invalid email or password");
        } catch (DisabledException ex) {
            throw new UnauthorizedException("Account is inactive");
        } catch (LockedException ex) {
            throw new UnauthorizedException("Account is blocked");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public MeResponse getCurrentUser() {
        Long userId = SecurityUtils.getCurrentUserId();

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        return authMapper.toMeResponse(user);
    }

    @Override
    public void register(RegisterRequest request) {

        if (!request.getPassword()
            .equals(request.getConfirmPassword())) {
            throw new BusinessException(
                "Password confirmation does not match",
                HttpStatus.BAD_REQUEST
            );
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(
                "Email already exists",
                HttpStatus.CONFLICT
            );
        }

        Role customerRole = roleRepository.findByName(
                UserRole.CUSTOMER.name())
            .orElseThrow(() ->
                             new ResourceNotFoundException(
                                 "Role",
                                 UserRole.CUSTOMER.name()
                             ));

        String email = request.getEmail().trim().toLowerCase();

        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        String otp = tx.execute(status -> {
            User user = new User();
            user.setFullName(request.getFullName());
            user.setEmail(email);
            user.setUsername(email);
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setRole(customerRole);
            user.setStatus(UserStatus.PENDING);
            userRepository.save(user);
            return otpService.generateOtp(email);
        });

        try {
            emailService.sendOtp(email, otp);
            log.info("Registration OTP sent to {}", email);
        } catch (Exception e) {
            log.error("Failed to send registration OTP to {}: {}", email, e.getMessage());
            throw new BusinessException(
                "Tai khoan da tao nhung gui OTP that bai. Vui long bam Gui lai OTP sau it phut.",
                HttpStatus.BAD_GATEWAY
            );
        }

        log.info("New customer registered (PENDING): {}", email);
    }

    @Override
    @Transactional
    public void resendOtp(String email) {

        String normalized = email == null ? "" : email.trim().toLowerCase();

        User user = userRepository.findByEmail(normalized)
            .orElseThrow(() -> new BusinessException(
                "Invalid request",
                HttpStatus.BAD_REQUEST
            ));

        if (user.getStatus() != UserStatus.PENDING) {
            throw new BusinessException(
                "Email already verified or account not eligible for OTP resend",
                HttpStatus.BAD_REQUEST
            );
        }

        String otp = otpService.resendOtp(normalized);

        emailService.sendOtp(normalized, otp);
    }

    @Override
    @Transactional
    public void verifyEmail(VerifyOtpRequest request) {
        if (request.getEmail() == null || request.getOtp() == null
            || request.getEmail().isBlank() || request.getOtp().isBlank()) {
            throw new BusinessException("Email and OTP are required", HttpStatus.BAD_REQUEST);
        }

        String email = request.getEmail().trim().toLowerCase();
        String otp = request.getOtp().trim();

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new BusinessException(
                "Invalid email or OTP",
                HttpStatus.BAD_REQUEST
            ));

        if (user.getStatus() != UserStatus.PENDING) {
            throw new BusinessException(
                "Email already verified or account cannot be activated with OTP",
                HttpStatus.BAD_REQUEST
            );
        }

        otpService.validateOtp(email, otp);

        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        log.info("Email verified, account activated: {}", email);
    }

    @Override
    @Transactional
    public void forgotPassword(String email) {

        String normalized = email == null ? "" : email.trim().toLowerCase();

        User user = userRepository.findByEmail(normalized)
            .orElseThrow(() ->
                             new BusinessException("Email does not exist", HttpStatus.BAD_REQUEST));

        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new BusinessException(
                "Account is blocked",
                HttpStatus.FORBIDDEN
            );
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(
                "Account is not active",
                HttpStatus.BAD_REQUEST
            );
        }

        String otp = otpService.generateOtp(normalized);
        emailService.sendResetPasswordOtp(normalized, otp);
    }

    @Override
    @Transactional
    public void verifyResetOtp(VerifyOtpRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() ->
                             new BusinessException("Invalid request", HttpStatus.BAD_REQUEST));

        otpService.validateOtp(request.getEmail(), request.getOtp());

    }

    @Override
    @Transactional
    public void updatePassword(UpdatePasswordRequest request) {

        if (!request.getNewPassword()
            .equals(request.getConfirmPassword())) {

            throw new BusinessException(
                "Password confirmation does not match",
                HttpStatus.BAD_REQUEST
            );
        }

        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() ->
                             new BusinessException("Invalid request", HttpStatus.BAD_REQUEST));

        EmailOtp latestOtp = emailOtpRepository
            .findTopByEmailOrderByIdDesc(request.getEmail())
            .orElseThrow(() ->
                             new BusinessException("Invalid request", HttpStatus.BAD_REQUEST));

        if (!latestOtp.isVerified()) {
            throw new BusinessException(
                "OTP verification required",
                HttpStatus.BAD_REQUEST
            );
        }

        if (latestOtp.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("OTP expired", HttpStatus.BAD_REQUEST);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        latestOtp.setVerified(false);
        emailOtpRepository.save(latestOtp);
    }
}

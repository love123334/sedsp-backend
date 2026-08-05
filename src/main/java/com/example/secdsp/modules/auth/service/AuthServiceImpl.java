package com.example.secdsp.modules.auth.service;

import com.example.secdsp.common.exception.*;
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
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;

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
        String email = request.getEmail() == null
            ? ""
            : request.getEmail().trim().toLowerCase();
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    email,
                    request.getPassword()
                )
            );

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

            User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", userDetails.getId()));

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
        } catch (UnauthorizedException ex) {
            throw ex;
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
                ErrorCode.INVALID_REQUEST,
                "Password confirmation does not match"
            );
        }

        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            User existing = userRepository.findByEmail(email).orElse(null);
            if (existing != null && existing.getStatus() == UserStatus.PENDING) {
                try {
                    String otp = otpService.resendOtp(email);
                    emailService.sendOtp(email, otp);
                } catch (Exception e) {
                    log.error("Resend OTP on re-register failed for {}: {}", email, e.getMessage());
                }
                // Account already pending — FE should show OTP form
                log.info("Pending account re-register; OTP form should be shown for {}", email);
                return;
            }
            throw new ResourceAlreadyExistsException("Email already exists");
        }

        Role customerRole = roleRepository.findByName(
                UserRole.CUSTOMER.name())
            .orElseThrow(() ->
                             new ResourceNotFoundException(
                                 "Role",
                                 UserRole.CUSTOMER.name()
                             ));

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
            // OTP already in DB — still return success so FE shows OTP + Resend
            log.error("Failed to send registration OTP to {}: {}", email, e.getMessage());
        }

        log.info("New customer registered (PENDING): {}", email);
    }

    @Override
    @Transactional
    public void resendOtp(String email) {

        String normalized = email == null ? "" : email.trim().toLowerCase();

        User user = userRepository.findByEmail(normalized)
            .orElseThrow(() -> new BusinessException(
                ErrorCode.INVALID_REQUEST,
                "Invalid request"
            ));

        if (user.getStatus() != UserStatus.PENDING) {
            throw new BusinessException(
                ErrorCode.BUSINESS_ERROR,
                "Email already verified or account not eligible for OTP resend"
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
            throw new BusinessException(
                ErrorCode.INVALID_REQUEST,
                "Email and OTP are required"
            );
        }

        String email = request.getEmail().trim().toLowerCase();
        String otp = request.getOtp().trim();

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new BusinessException(
                ErrorCode.INVALID_REQUEST,
                "Invalid email or OTP"
            ));

        if (user.getStatus() != UserStatus.PENDING) {
            throw new BusinessException(
                ErrorCode.BUSINESS_ERROR,
                "Email already verified or account cannot be activated with OTP"
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
            .orElseThrow(() -> new BusinessException(
                ErrorCode.INVALID_REQUEST,
                "Email does not exist"
            ));

        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new ForbiddenException("Account is blocked");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(
                ErrorCode.BUSINESS_ERROR,
                "Account is not active"
            );
        }

        String otp = otpService.generateOtp(normalized);
        emailService.sendResetPasswordOtp(normalized, otp);
    }

    @Override
    @Transactional
    public void verifyResetOtp(VerifyOtpRequest request) {

        String email = request.getEmail() == null
            ? ""
            : request.getEmail().trim().toLowerCase();

        userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException(
                "User with email",
                email
            ));

        otpService.validateOtp(email, request.getOtp());

    }

    @Override
    @Transactional
    public void updatePassword(UpdatePasswordRequest request) {

        if (!request.getNewPassword()
            .equals(request.getConfirmPassword())) {

            throw new BusinessException(
                ErrorCode.INVALID_REQUEST,
                "Password confirmation does not match"
            );
        }

        String email = request.getEmail() == null
            ? ""
            : request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException(
                "User with email",
                email
            ));

        EmailOtp latestOtp = emailOtpRepository
            .findTopByEmailOrderByIdDesc(email)
            .orElseThrow(() -> new BusinessException(
                ErrorCode.INVALID_REQUEST,
                "OTP not found"
            ));

        if (!latestOtp.isVerified()) {
            throw new BusinessException(
                ErrorCode.BUSINESS_ERROR,
                "OTP verification required"
            );
        }

        if (latestOtp.getExpiryTime().isBefore(OffsetDateTime.now())) {
            throw new BusinessException(
                ErrorCode.BUSINESS_ERROR,
                "OTP expired"
            );
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        latestOtp.setVerified(false);
        emailOtpRepository.save(latestOtp);
    }
}

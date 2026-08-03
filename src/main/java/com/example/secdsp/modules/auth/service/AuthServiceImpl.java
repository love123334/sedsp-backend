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
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
    public void register(RegisterRequest request) {

        if (!request.getPassword()
            .equals(request.getConfirmPassword())) {
            throw new BusinessException(
                ErrorCode.INVALID_REQUEST,
                "Password confirmation does not match"
            );
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceAlreadyExistsException("Email already exists");
        }

        Role customerRole = roleRepository.findByName(
                UserRole.CUSTOMER.name())
            .orElseThrow(() ->
                             new ResourceNotFoundException(
                                 "Role",
                                 UserRole.CUSTOMER.name()
                             ));

        User user = new User();
        user.setFullName(request.getFullName());

        String email = request.getEmail().trim().toLowerCase();

        user.setEmail(email);

        user.setUsername(email);

        user.setPassword(
            passwordEncoder.encode(
                request.getPassword()
            )
        );

        user.setRole(customerRole);
        user.setStatus(UserStatus.PENDING);

        userRepository.save(user);

        String otp = otpService.generateOtp(email);
        emailService.sendOtp(email, otp);

        log.info("New customer registered: {}", email);
    }

    @Override
    @Transactional
    public void resendOtp(String email) {

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException(
                "User with email",
                email
            ));

        if (user.getStatus() != UserStatus.PENDING) {
            throw new BusinessException(
                ErrorCode.BUSINESS_ERROR,
                "Email already verified or account not eligible for OTP resend"
            );
        }

        String otp = otpService.resendOtp(email);

        emailService.sendOtp(email, otp);
    }

    @Override
    @Transactional
    public void forgotPassword(String email) {

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException(
                "User with email",
                email
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

        String otp = otpService.generateOtp(email);

        emailService.sendResetPasswordOtp(email, otp);
    }

    @Override
    @Transactional
    public void verifyResetOtp(VerifyOtpRequest request) {

        userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new ResourceNotFoundException(
                "User with email",
                request.getEmail()
            ));

        otpService.validateOtp(request.getEmail(), request.getOtp());

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

        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new ResourceNotFoundException(
                "User with email",
                request.getEmail()
            ));

        EmailOtp latestOtp = emailOtpRepository
            .findTopByEmailOrderByIdDesc(request.getEmail())
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

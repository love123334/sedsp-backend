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
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
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

        String otp = otpService.resendOtp(email);

        emailService.sendOtp(email, otp);
    }

    @Override
    @Transactional
    public void forgotPassword(String email) {

        User user = userRepository.findByEmail(email)
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

        String otp = otpService.generateOtp(email);

        emailService.sendResetPasswordOtp(email, otp);
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

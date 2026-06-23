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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (BadCredentialsException ex) {
            log.warn("Login failed for email: {}", request.getEmail());
            throw new UnauthorizedException("Invalid email or password");
        }

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String accessToken = jwtProvider.generateAccessToken(userDetails.getId());

        long expiresInSeconds = jwtExpirationMs / 1000;
        return authMapper.toLoginResponse(
            accessToken,
            expiresInSeconds,
            userDetails.getId(),
            userDetails.getEmail(),
            userDetails.getRole()
        );
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
        user.setStatus(UserStatus.ACTIVE);

        userRepository.save(user);

        log.info("New customer registered: {}", email);
    }
}

package com.example.secdsp.security.oauth2;

import com.example.secdsp.modules.user.entity.*;
import com.example.secdsp.modules.user.repository.RoleRepository;
import com.example.secdsp.modules.user.repository.UserRepository;
import com.example.secdsp.security.jwt.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtProvider jwtProvider;

    @Value("${app.oauth2.redirect-uri}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
        throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        User user = userRepository.findByEmail(email)
            .orElseGet(() -> createNewUser(email, name));

        String token = jwtProvider.generateAccessToken(user.getId());

        String redirectUrl = redirectUri + "?token=" + token;

        response.sendRedirect(redirectUrl);
    }

    private User createNewUser(String email, String name) {

        Role customerRole = roleRepository.findByName(UserRole.CUSTOMER.name())
            .orElseThrow();

        User user = new User();
        user.setEmail(email);
        user.setUsername(email);
        user.setFullName(name);
        user.setPassword(""); // không dùng password
        user.setRole(customerRole);
        user.setStatus(UserStatus.ACTIVE);

        return userRepository.save(user);
    }
}
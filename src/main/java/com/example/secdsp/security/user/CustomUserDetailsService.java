package com.example.secdsp.security.user;

import com.example.secdsp.modules.user.entity.User;
import com.example.secdsp.modules.user.entity.UserRole;
import com.example.secdsp.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> {
                log.warn(
                    "User not found with email: {}",
                    email
                );
                return new UsernameNotFoundException(
                    "User not found"
                );
            });

        UserRole role = getUserRole(user);

        return UserDetailsImpl.build(
            user.getId(),
            user.getEmail(),
            user.getPassword(),
            role,
            user.getStatus()
        );
    }

    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long id) {

        User user = userRepository.findById(id)
            .orElseThrow(() ->
                             new UsernameNotFoundException(
                                 "User not found"
                             ));

        UserRole role = getUserRole(user);

        return UserDetailsImpl.build(
            user.getId(),
            user.getEmail(),
            user.getPassword(),
            role,
            user.getStatus()
        );
    }

    private UserRole getUserRole(User user) {

        if (user.getRole() == null) {
            log.warn(
                "User {} has no role assigned",
                user.getEmail()
            );
            throw new UsernameNotFoundException(
                "User has no role assigned"
            );
        }

        try {
            return UserRole.valueOf(
                user.getRole().getName()
            );
        } catch (IllegalArgumentException ex) {
            log.error(
                "Invalid role '{}' for user {}",
                user.getRole().getName(),
                user.getEmail()
            );
            throw new UsernameNotFoundException(
                "Invalid user role"
            );
        }
    }
}
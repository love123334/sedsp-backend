package com.example.secdsp.security.user;

import com.example.secdsp.modules.user.entity.UserRole;
import com.example.secdsp.modules.user.entity.UserStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
@Builder
public class UserDetailsImpl implements UserDetails {

    private final Long id;
    private final String email;
    private final UserRole role;
    private final UserStatus status;

    @Getter(AccessLevel.NONE)
    private final String password;

    public static UserDetailsImpl build(Long id, String email, String password, UserRole role, UserStatus status) {
        return UserDetailsImpl.builder()
            .id(id)
            .email(email)
            .password(password)
            .role(role)
            .status(status)
            .build();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email != null ? email : String.valueOf(id);
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != UserStatus.BLOCKED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        // PENDING must authenticate so login can return "verify email" (not DisabledException).
        return status == UserStatus.ACTIVE || status == UserStatus.PENDING;
    }
}
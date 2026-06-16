package com.example.secdsp.modules.auth.mapper;

import com.example.secdsp.modules.auth.dto.response.CurrentUserSummary;
import com.example.secdsp.modules.auth.dto.response.LoginResponse;
import com.example.secdsp.modules.auth.dto.response.MeResponse;
import com.example.secdsp.modules.user.entity.User;
import com.example.secdsp.modules.user.entity.UserRole;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuthMapper {

    @Mapping(target = "role", expression = "java(user.getRole() != null ? user.getRole().name() : null)")
    CurrentUserSummary toCurrentUserSummary(User user);

    @Mapping(target = "role", expression = "java(user.getRole() != null ? user.getRole().name() : null)")
    MeResponse toMeResponse(User user);

    default LoginResponse toLoginResponse(String accessToken, long expiresInSeconds,
                                          Long id, String email, UserRole role) {
        return LoginResponse.builder()
            .tokenType("Bearer")
            .accessToken(accessToken)
            .expiresInSeconds(expiresInSeconds)
            .user(CurrentUserSummary.builder()
                      .id(id)
                      .email(email)
                      .role(role != null ? role.name() : null)
                      .build())
            .build();
    }
}
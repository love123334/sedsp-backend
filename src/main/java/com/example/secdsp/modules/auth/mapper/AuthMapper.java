package com.example.secdsp.modules.auth.mapper;

import com.example.secdsp.modules.auth.dto.response.CurrentUserSummary;
import com.example.secdsp.modules.auth.dto.response.LoginResponse;
import com.example.secdsp.modules.auth.dto.response.MeResponse;
import com.example.secdsp.modules.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuthMapper {

    @Mapping(target = "role", source = "role.name")
    CurrentUserSummary toCurrentUserSummary(User user);

    @Mapping(target = "role", source = "role.name")
    MeResponse toMeResponse(User user);

    default LoginResponse toLoginResponse(String accessToken, long expiresInSeconds, Long id, String email, String role) {
        return LoginResponse.builder()
            .tokenType("Bearer")
            .accessToken(accessToken)
            .expiresInSeconds(expiresInSeconds)
            .user(CurrentUserSummary.builder()
                      .id(id)
                      .email(email)
                      .role(role)
                      .build())
            .build();
    }
}

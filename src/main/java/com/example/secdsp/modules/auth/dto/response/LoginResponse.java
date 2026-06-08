package com.example.secdsp.modules.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {

    private String tokenType;
    private String accessToken;
    private long expiresInSeconds;
    private CurrentUserSummary user;
}

package com.example.secdsp.modules.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CurrentUserSummary {

    private Long id;
    private String email;
    private String username;
    private String role;
}

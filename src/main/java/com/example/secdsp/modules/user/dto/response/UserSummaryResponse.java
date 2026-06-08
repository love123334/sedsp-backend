package com.example.secdsp.modules.user.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserSummaryResponse {

    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String role;
    private String status;
}

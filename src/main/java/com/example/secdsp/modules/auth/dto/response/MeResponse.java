package com.example.secdsp.modules.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MeResponse {

    private Long id;
    private String email;
    private String username;
    private String fullName;
    private String phone;
    private String role;
}

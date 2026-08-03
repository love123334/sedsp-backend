package com.example.secdsp.modules.auth.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MeResponse {

    Long id;
    String email;
    String username;
    String fullName;
    String phone;
    String role;
}

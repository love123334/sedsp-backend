package com.example.secdsp.modules.email.dto.request;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdatePasswordRequest {

    String email;
    String newPassword;
    String confirmPassword;
}
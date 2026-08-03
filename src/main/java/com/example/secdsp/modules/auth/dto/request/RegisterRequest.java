package com.example.secdsp.modules.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RegisterRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 150)
    String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 150)
    String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must contain at least 8 characters")
    String password;

    @NotBlank(message = "Confirm password is required")
    String confirmPassword;
}

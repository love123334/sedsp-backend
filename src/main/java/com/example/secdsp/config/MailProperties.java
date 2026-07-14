package com.example.secdsp.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.mail")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MailProperties {

    String from;
    String logoPath;
    int otpExpirationMinutes;
    int resendCooldownSeconds;
    int maxResendAttempts;
}
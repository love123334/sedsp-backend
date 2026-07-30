package com.example.secdsp.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.payment.momo")
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MoMoProperties {

    String partnerCode;
    String accessKey;
    String secretKey;
    String endpoint;
    String returnUrl;
    String notifyUrl;
    String requestType;
}

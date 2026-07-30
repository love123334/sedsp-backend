package com.example.secdsp.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.payment.vnpay")
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VnPayProperties {

    String tmnCode;
    String secretKey;
    String payUrl;
    String returnUrl;
    String ipnUrl;
    String version;
    String command;
}

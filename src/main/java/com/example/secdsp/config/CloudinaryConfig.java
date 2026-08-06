package com.example.secdsp.config;

import com.cloudinary.Cloudinary;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "app.cloudinary")
@Getter
@Setter
public class CloudinaryConfig {

    private String cloudName;
    private String apiKey;
    private String apiSecret;

    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(Map.of(
            "cloud_name", cloudName != null ? cloudName : "",
            "api_key", apiKey != null ? apiKey : "",
            "api_secret", apiSecret != null ? apiSecret : ""
        ));
    }

    public boolean isConfigured() {
        return cloudName != null && !cloudName.isBlank() && !cloudName.startsWith("your_")
            && apiKey != null && !apiKey.isBlank() && !apiKey.startsWith("your_")
            && apiSecret != null && !apiSecret.isBlank() && !apiSecret.startsWith("your_");
    }
}
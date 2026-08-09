package com.example.secdsp.config;

import com.cloudinary.Cloudinary;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Configuration
@ConfigurationProperties(prefix = "app.cloudinary")
@Getter
@Setter
public class CloudinaryConfig {

    /** Placeholders / project names that are NOT real Cloudinary cloud_name values. */
    private static final Set<String> INVALID_CLOUD_NAMES = Set.of(
        "sedsp",
        "secdsp",
        "your_cloud_name",
        "your-cloud-name",
        "cloud_name",
        "demo",
        "changeme",
        "example",
        "test",
        "localhost"
    );

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
        return isValidCredential(cloudName)
            && isValidCredential(apiKey)
            && isValidCredential(apiSecret)
            && !INVALID_CLOUD_NAMES.contains(cloudName.trim().toLowerCase(Locale.ROOT));
    }

    private static boolean isValidCredential(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String v = value.trim();
        return !v.startsWith("your_")
            && !v.startsWith("YOUR_")
            && !v.equalsIgnoreCase("changeme");
    }
}

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
        if (!isValidCredential(cloudName)
            || !isValidCredential(apiKey)
            || !isValidCredential(apiSecret)) {
            return false;
        }
        String normalized = cloudName.trim().toLowerCase(Locale.ROOT);
        if (INVALID_CLOUD_NAMES.contains(normalized)) {
            return false;
        }
        // Real Cloudinary cloud_name values are lowercase (e.g. flvb615r), not project labels like SEDSP.
        if (cloudName.equals(cloudName.toUpperCase(Locale.ROOT))) {
            return false;
        }
        return true;
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

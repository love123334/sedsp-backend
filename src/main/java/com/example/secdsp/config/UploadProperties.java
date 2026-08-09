package com.example.secdsp.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConfigurationProperties(prefix = "app.upload")
@Getter
@Setter
public class UploadProperties {

    /** Directory for product images when Cloudinary is unavailable. */
    private String dir = "uploads";

    /**
     * Public base URL of this API (no trailing slash), e.g. https://sedsp-backend.up.railway.app
     * Used to build absolute image URLs for the frontend (Vercel).
     */
    private String publicBaseUrl = "";

    public String resolvePublicBaseUrl() {
        if (StringUtils.hasText(publicBaseUrl)) {
            return trimSlash(publicBaseUrl.trim());
        }
        String railway = System.getenv("RAILWAY_PUBLIC_DOMAIN");
        if (StringUtils.hasText(railway)) {
            String host = railway.trim();
            if (host.startsWith("http://") || host.startsWith("https://")) {
                return trimSlash(host);
            }
            return "https://" + trimSlash(host);
        }
        return "http://localhost:8080";
    }

    private static String trimSlash(String url) {
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }
}

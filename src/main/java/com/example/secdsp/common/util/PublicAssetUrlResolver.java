package com.example.secdsp.common.util;

import com.example.secdsp.config.UploadProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;

/**
 * Rewrites localhost / relative upload paths to the public API base URL
 * so Vercel clients can load images stored during local dev or on ephemeral disk.
 */
@Component
@RequiredArgsConstructor
public class PublicAssetUrlResolver {

    private final UploadProperties uploadProperties;

    public String resolve(String url) {
        if (!StringUtils.hasText(url)) {
            return url;
        }
        String trimmed = url.trim();
        String base = uploadProperties.resolvePublicBaseUrl();

        if (trimmed.startsWith("/uploads/")) {
            return base + trimmed;
        }

        if (trimmed.contains("localhost") || trimmed.contains("127.0.0.1")) {
            try {
                URI uri = URI.create(trimmed);
                if (uri.getPath() != null && uri.getPath().startsWith("/uploads/")) {
                    String path = uri.getPath();
                    String query = uri.getQuery();
                    return query == null || query.isBlank()
                        ? base + path
                        : base + path + "?" + query;
                }
            } catch (IllegalArgumentException ignored) {
                /* keep original */
            }
        }

        return trimmed;
    }
}

package com.example.secdsp.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * DeepSeek OpenAI-compatible API — token only on server (DEEPSEEK_API_KEY).
 */
@Component
@ConfigurationProperties(prefix = "app.deepseek")
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DeepSeekProperties {

    /** When blank, DeepSeek is off. Enabled automatically if api-key is set. */
    boolean enabled = true;

    String apiKey = "";

    String baseUrl = "https://api.deepseek.com";

    /** deepseek-chat or deepseek-reasoner */
    String model = "deepseek-chat";

    double temperature = 0.4;

    int maxTokens = 1200;

    /** Off by default — polish adds 3–6s after Gemini. */
    boolean refineGemini = false;

    public boolean isConfigured() {
        return enabled
            && StringUtils.hasText(apiKey)
            && !apiKey.startsWith("YOUR_")
            && !apiKey.contains("changeme");
    }
}

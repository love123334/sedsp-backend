package com.example.secdsp.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.ai")
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiProperties {

    boolean enabled = false;

  /** Hugging Face / OpenRouter token (OPENROUTER_API_KEY or HF_API_TOKEN) — never expose to FE */
  String apiToken = "";

  /** OpenAI-compatible base, e.g. https://openrouter.ai/api/v1 */
  String baseUrl = "https://openrouter.ai/api/v1";

  /** e.g. openai/gpt-4o-mini */
  String model = "openrouter/free";

    double temperature = 0.6;

    int maxTokens = 900;
}

package com.example.secdsp.config;

import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GoogleAiConfig {

    @Bean
    @ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${google.ai.api-key:}')")
    public Client googleAiClient(
        @Value("${google.ai.api-key}") String apiKey
    ) {
        return Client.builder()
            .apiKey(apiKey)
            .httpOptions(
                HttpOptions.builder()
                    .timeout(8_000)
                    .build()
            )
            .build();
    }
}
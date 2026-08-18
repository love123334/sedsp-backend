package com.example.secdsp.config;

import com.google.genai.Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GoogleAiConfig {

    @Bean
    public Client googleAiClient(
        @Value("${google.ai.api-key}") String apiKey
    ) {
        return Client.builder()
            .apiKey(apiKey)
            .build();
    }
}
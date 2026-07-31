package com.example.secdsp.modules.ai.dto;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiChatResponse {

    String content;
    String provider;
    String model;
    boolean fallback;
}

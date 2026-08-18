package com.example.secdsp.modules.ai.service;

import com.example.secdsp.modules.ai.dto.AiChatRequest;
import com.example.secdsp.modules.ai.dto.AiChatResponse;

public interface AiChatService {

    AiChatResponse chat(AiChatRequest request);
}
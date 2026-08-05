package com.example.secdsp.modules.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiChatRequest {

    @NotEmpty
    @Size(max = 24, message = "Too many chat turns (max 24)")
    @Valid
    List<ChatTurn> messages;

    @Getter
    @Setter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ChatTurn {
        @NotBlank
        @Size(max = 32)
        String role;

        @NotBlank
        @Size(max = 8000, message = "Message content too long (max 8000)")
        String content;
    }
}

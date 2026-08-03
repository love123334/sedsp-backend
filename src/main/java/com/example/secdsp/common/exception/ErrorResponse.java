package com.example.secdsp.common.exception;

import com.example.secdsp.common.exception.dto.FieldErrorResponse;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ErrorResponse {

    Instant timestamp;

    Integer status;

    String error;

    String code;

    String message;

    String path;

    List<FieldErrorResponse> errors;
}

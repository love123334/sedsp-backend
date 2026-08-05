package com.example.secdsp.common.exception.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(
    name = "FieldErrorResponse",
    description = "Validation or business error details"
)
public class FieldErrorResponse {

    @Schema(
        description = "The field that caused the validation error",
        example = "email"
    )
    String field;

    @Schema(
        description = "Description of the validation error",
        example = "Email must not be blank"
    )
    String message;
}
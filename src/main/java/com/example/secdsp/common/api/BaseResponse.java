package com.example.secdsp.common.api;

import com.example.secdsp.common.exception.dto.FieldErrorResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
    name = "BaseResponse",
    description = "Standard API response wrapper"
)
public class BaseResponse<T> {

    @Schema(
        description = "Indicates whether the request was processed successfully",
        example = "true"
    )
    boolean success;

    @Schema(
        description = "Response message",
        example = "Request successful"
    )
    String message;

    @Schema(
        description = "Response payload. The actual type depends on the endpoint."
    )
    T data;

    @Schema(
        description = "List of validation or business errors. Present only when success is false."
    )
    List<FieldErrorResponse> errors;

    public static <T> BaseResponse<T> success(T data) {
        return BaseResponse.<T>builder()
            .success(true)
            .message("Request successful")
            .data(data)
            .build();
    }

    public static <T> BaseResponse<T> success(String message, T data) {
        return BaseResponse.<T>builder()
            .success(true)
            .message(message)
            .data(data)
            .build();
    }

    public static <T> BaseResponse<T> success(String message) {
        return BaseResponse.<T>builder()
            .success(true)
            .message(message)
            .build();
    }

    public static BaseResponse<Void> error(String message) {
        return BaseResponse.<Void>builder()
            .success(false)
            .message(message)
            .build();
    }

    public static BaseResponse<Void> error(String message, List<FieldErrorResponse> errors) {
        return BaseResponse.<Void>builder()
            .success(false)
            .message(message)
            .errors(errors)
            .build();
    }
}
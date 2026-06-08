package com.example.secdsp.common.exception.dto;

import jakarta.validation.ConstraintViolation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.validation.FieldError;

import java.util.List;
import java.util.Set;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationErrorResponse {

    private List<FieldErrorResponse> errors;

    public static List<FieldErrorResponse> fromFieldErrors(List<FieldError> fieldErrors) {
        return fieldErrors.stream()
                .map(error -> FieldErrorResponse.builder()
                        .field(error.getField())
                        .message(error.getDefaultMessage())
                        .build())
                .toList();
    }

    public static List<FieldErrorResponse> fromConstraintViolations(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream()
                .map(violation -> FieldErrorResponse.builder()
                        .field(extractFieldName(violation))
                        .message(violation.getMessage())
                        .build())
                .toList();
    }

    private static String extractFieldName(ConstraintViolation<?> violation) {
        String propertyPath = violation.getPropertyPath().toString();
        int lastDotIndex = propertyPath.lastIndexOf('.');
        return lastDotIndex >= 0 ? propertyPath.substring(lastDotIndex + 1) : propertyPath;
    }
}

package com.example.secdsp.common.handler;

import com.example.secdsp.common.api.BaseResponse;
import com.example.secdsp.common.exception.BusinessException;
import com.example.secdsp.common.exception.dto.FieldErrorResponse;
import com.example.secdsp.common.exception.dto.ValidationErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String VALIDATION_FAILED = "Validation failed";
    private static final String MALFORMED_REQUEST = "Malformed request body";
    private static final String INTERNAL_SERVER_ERROR = "An unexpected error occurred";
    private static final String ACCESS_DENIED = "Access denied";
    private static final String AUTHENTICATION_FAILED = "Authentication failed";
    private static final String METHOD_NOT_ALLOWED = "HTTP method is not supported";
    private static final String DATA_INTEGRITY_VIOLATION = "Data integrity violation";

    /**
     * =========================
     * Validation Exceptions
     * =========================
     */

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<Void>> handleMethodArgumentNotValid(
        MethodArgumentNotValidException ex
    ) {

        List<FieldErrorResponse> errors =
            ValidationErrorResponse.fromFieldErrors(ex.getBindingResult().getFieldErrors());

        log.warn("[400] Validation failed: {}", errors);

        return buildValidationResponse(errors);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<BaseResponse<Void>> handleBindException(
        BindException ex
    ) {

        List<FieldErrorResponse> errors =
            ValidationErrorResponse.fromFieldErrors(ex.getBindingResult().getFieldErrors());

        log.warn("[400] Validation failed: {}", errors);

        return buildValidationResponse(errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<BaseResponse<Void>> handleConstraintViolation(
        ConstraintViolationException ex
    ) {

        List<FieldErrorResponse> errors =
            ValidationErrorResponse.fromConstraintViolations(ex.getConstraintViolations());

        log.warn("[400] Validation failed: {}", errors);

        return buildValidationResponse(errors);
    }

    /**
     * =========================
     * Request Exceptions
     * =========================
     */

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<BaseResponse<Void>> handleHttpMessageNotReadable(
        HttpMessageNotReadableException ex
    ) {

        log.warn("[400] Malformed request body", ex);

        return buildErrorResponse(HttpStatus.BAD_REQUEST, MALFORMED_REQUEST);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<BaseResponse<Void>> handleMissingServletRequestParameter(
        MissingServletRequestParameterException ex
    ) {

        String message = String.format(
            "Required parameter '%s' is missing",
            ex.getParameterName()
        );

        log.warn("[400] {}", message);

        return buildErrorResponse(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(MissingPathVariableException.class)
    public ResponseEntity<BaseResponse<Void>> handleMissingPathVariable(
        MissingPathVariableException ex
    ) {

        String message = String.format(
            "Required path variable '%s' is missing",
            ex.getVariableName()
        );

        log.warn("[400] {}", message);

        return buildErrorResponse(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<BaseResponse<Void>> handleMethodArgumentTypeMismatch(
        MethodArgumentTypeMismatchException ex
    ) {

        String message = String.format(
            "Invalid value '%s' for parameter '%s'",
            ex.getValue(),
            ex.getName()
        );

        log.warn("[400] {}", message);

        return buildErrorResponse(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<BaseResponse<Void>> handleMethodNotSupported(
        HttpRequestMethodNotSupportedException ex
    ) {

        log.warn("[405] {}", ex.getMessage());

        return buildErrorResponse(
            HttpStatus.METHOD_NOT_ALLOWED,
            METHOD_NOT_ALLOWED
        );
    }

    /**
     * =========================
     * Security Exceptions
     * =========================
     */

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<BaseResponse<Void>> handleAuthenticationException(
        AuthenticationException ex
    ) {

        log.warn("[401] {}", ex.getMessage());

        return buildErrorResponse(
            HttpStatus.UNAUTHORIZED,
            AUTHENTICATION_FAILED
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<BaseResponse<Void>> handleAccessDeniedException(
        AccessDeniedException ex
    ) {

        log.warn("[403] {}", ex.getMessage());

        return buildErrorResponse(
            HttpStatus.FORBIDDEN,
            ACCESS_DENIED
        );
    }

    /**
     * =========================
     * Database Exceptions
     * =========================
     */

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<BaseResponse<Void>> handleDataIntegrityViolation(
        DataIntegrityViolationException ex
    ) {

        log.error("[409] Data integrity violation", ex);

        return buildErrorResponse(
            HttpStatus.CONFLICT,
            DATA_INTEGRITY_VIOLATION
        );
    }

    /**
     * =========================
     * Business Exceptions
     * =========================
     */

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<BaseResponse<Void>> handleBusinessException(
        BusinessException ex
    ) {

        log.warn(
            "[{}] {}",
            ex.getErrorCode().getHttpStatus().value(),
            ex.getMessage()
        );

        return buildErrorResponse(
            ex.getErrorCode().getHttpStatus(),
            ex.getMessage()
        );
    }

    /**
     * =========================
     * Fallback
     * =========================
     */

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Void>> handleUnexpectedException(
        Exception ex
    ) {

        log.error("[500] Unexpected error", ex);

        return buildErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            INTERNAL_SERVER_ERROR
        );
    }

    /**
     * =========================
     * Helper Methods
     * =========================
     */

    private ResponseEntity<BaseResponse<Void>> buildErrorResponse(
        HttpStatus status,
        String message
    ) {

        return ResponseEntity
            .status(status)
            .body(BaseResponse.error(message));
    }

    private ResponseEntity<BaseResponse<Void>> buildValidationResponse(
        List<FieldErrorResponse> errors
    ) {

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(BaseResponse.error(VALIDATION_FAILED, errors));
    }
}
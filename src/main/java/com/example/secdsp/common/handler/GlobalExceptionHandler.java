package com.example.secdsp.common.handler;

import com.example.secdsp.common.api.BaseResponse;
import com.example.secdsp.common.exception.BusinessException;
import com.example.secdsp.common.exception.dto.FieldErrorResponse;
import com.example.secdsp.common.exception.dto.ValidationErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String VALIDATION_FAILED_MESSAGE = "Validation failed";
    private static final String UNEXPECTED_ERROR_MESSAGE = "An unexpected error occurred";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        List<FieldErrorResponse> errors = ValidationErrorResponse.fromFieldErrors(ex.getBindingResult().getFieldErrors());
        log.warn("Validation failed: {}", errors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.error(VALIDATION_FAILED_MESSAGE, errors));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<BaseResponse<Void>> handleBindException(BindException ex) {
        List<FieldErrorResponse> errors = ValidationErrorResponse.fromFieldErrors(ex.getBindingResult().getFieldErrors());
        log.warn("Validation failed: {}", errors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.error(VALIDATION_FAILED_MESSAGE, errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<BaseResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        List<FieldErrorResponse> errors = ValidationErrorResponse.fromConstraintViolations(ex.getConstraintViolations());
        log.warn("Validation failed: {}", errors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.error(VALIDATION_FAILED_MESSAGE, errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<BaseResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Malformed request body: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.error("Malformed request body"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<BaseResponse<Void>> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex) {
        log.warn("Missing request parameter: {}", ex.getParameterName());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.error(String.format("Required parameter '%s' is missing", ex.getParameterName())));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<BaseResponse<Void>> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex) {
        log.warn("Invalid parameter type for '{}': {}", ex.getName(), ex.getValue());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.error(String.format("Invalid value for parameter '%s'", ex.getName())));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<BaseResponse<Void>> handleBusinessException(BusinessException ex) {
        log.warn("Business exception: {}", ex.getMessage());
        return ResponseEntity
                .status(ex.getHttpStatus())
                .body(BaseResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Void>> handleUnexpectedException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(BaseResponse.error(UNEXPECTED_ERROR_MESSAGE));
    }
}

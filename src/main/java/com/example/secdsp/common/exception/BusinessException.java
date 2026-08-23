package com.example.secdsp.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /** Compatibility shim for call sites that only pass a message. */
    public BusinessException(String message) {
        this(ErrorCode.BUSINESS_ERROR, message);
    }

    /** Compatibility shim for call sites that pass message + HTTP status. */
    public BusinessException(String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = fromHttpStatus(httpStatus);
    }

    private static ErrorCode fromHttpStatus(HttpStatus httpStatus) {
        if (httpStatus == null) {
            return ErrorCode.BUSINESS_ERROR;
        }
        if (httpStatus == HttpStatus.NOT_FOUND) {
            return ErrorCode.RESOURCE_NOT_FOUND;
        }
        if (httpStatus == HttpStatus.UNAUTHORIZED) {
            return ErrorCode.UNAUTHORIZED;
        }
        if (httpStatus == HttpStatus.FORBIDDEN) {
            return ErrorCode.ACCESS_DENIED;
        }
        if (httpStatus == HttpStatus.CONFLICT) {
            return ErrorCode.RESOURCE_ALREADY_EXISTS;
        }
        if (httpStatus == HttpStatus.BAD_REQUEST) {
            return ErrorCode.BUSINESS_ERROR;
        }
        if (httpStatus == HttpStatus.TOO_MANY_REQUESTS) {
            return ErrorCode.BUSINESS_ERROR;
        }
        if (httpStatus.is5xxServerError()) {
            return ErrorCode.INTERNAL_SERVER_ERROR;
        }
        return ErrorCode.BUSINESS_ERROR;
    }
}

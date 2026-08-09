package com.example.secdsp.common.exception;

import lombok.Getter;

/**
 * Exception thrown when email operations fail.
 */
@Getter
public class EmailServiceException extends BusinessException {

    public EmailServiceException(String message) {
        super(ErrorCode.EMAIL_SERVICE_ERROR, message);
    }

    public EmailServiceException(String message, Throwable cause) {
        super(ErrorCode.EMAIL_SERVICE_ERROR, message);
        this.initCause(cause);
    }
}

package com.example.secdsp.common.exception;

public class ForbiddenException extends BusinessException {

    public ForbiddenException() {
        super(ErrorCode.ACCESS_DENIED);
    }

    public ForbiddenException(String message) {
        super(ErrorCode.ACCESS_DENIED, message);
    }
}
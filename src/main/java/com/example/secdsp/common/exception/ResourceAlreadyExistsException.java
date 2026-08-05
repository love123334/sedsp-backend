package com.example.secdsp.common.exception;

public class ResourceAlreadyExistsException extends BusinessException {

    public ResourceAlreadyExistsException(String message) {
        super(ErrorCode.RESOURCE_ALREADY_EXISTS, message);
    }
}
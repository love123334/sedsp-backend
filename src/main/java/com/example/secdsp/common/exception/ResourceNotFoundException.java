package com.example.secdsp.common.exception;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message);
    }

    public ResourceNotFoundException(String resourceName, Object identifier) {
        super(
            ErrorCode.RESOURCE_NOT_FOUND,
            String.format("%s not found with identifier: %s", resourceName, identifier)
        );
    }
}
package com.example.secdsp.common.exception;

import lombok.Getter;

/**
 * Exception thrown when Cloudinary operations fail.
 */
@Getter
public class CloudinaryException extends BusinessException {

    public CloudinaryException(String message) {
        super(ErrorCode.CLOUDINARY_ERROR, message);
    }

    public CloudinaryException(String message, Throwable cause) {
        super(ErrorCode.CLOUDINARY_ERROR, message);
        this.initCause(cause);
    }
}

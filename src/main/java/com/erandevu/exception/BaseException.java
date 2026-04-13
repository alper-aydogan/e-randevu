package com.erandevu.exception;

import lombok.Getter;

import java.util.Map;

/**
 * Abstract base exception for all application-specific exceptions.
 * Provides standardized error code support and structured error details.
 */
@Getter
public abstract class BaseException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Map<String, Object> details;

    protected BaseException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.details = null;
    }

    protected BaseException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.details = null;
    }

    protected BaseException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = null;
    }

    protected BaseException(ErrorCode errorCode, String message, Map<String, Object> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }

    protected BaseException(ErrorCode errorCode, String message, Map<String, Object> details, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = details;
    }

    /**
     * Get HTTP status code from error code.
     */
    public int getHttpStatus() {
        return errorCode.getHttpStatus().value();
    }

    /**
     * Check if this is a system error (needs investigation).
     */
    public boolean isSystemError() {
        return errorCode.getCategory() == ErrorCode.ErrorCategory.SYSTEM;
    }

    /**
     * Check if this is a client error (expected).
     */
    public boolean isClientError() {
        return !isSystemError();
    }
}

package com.erandevu.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Type-safe error code enumeration for standardized API error handling.
 * Organized by error taxonomy: Business, Validation, Security, System.
 */
@Getter
public enum ErrorCode {

    // ==================== BUSINESS ERRORS (4xx) ====================
    RESOURCE_NOT_FOUND("E001", "Resource not found", HttpStatus.NOT_FOUND, ErrorCategory.BUSINESS),
    USER_ALREADY_EXISTS("E002", "User already exists", HttpStatus.CONFLICT, ErrorCategory.BUSINESS),
    APPOINTMENT_CONFLICT("E003", "Appointment conflict detected", HttpStatus.CONFLICT, ErrorCategory.BUSINESS),
    BUSINESS_RULE_VIOLATION("E004", "Business rule violation", HttpStatus.BAD_REQUEST, ErrorCategory.BUSINESS),
    APPOINTMENT_TIME_INVALID("E005", "Invalid appointment time", HttpStatus.BAD_REQUEST, ErrorCategory.BUSINESS),

    // ==================== VALIDATION ERRORS (400) ====================
    VALIDATION_ERROR("V001", "Request validation failed", HttpStatus.BAD_REQUEST, ErrorCategory.VALIDATION),
    INVALID_INPUT("V002", "Invalid input provided", HttpStatus.BAD_REQUEST, ErrorCategory.VALIDATION),
    MISSING_REQUIRED_FIELD("V003", "Required field missing", HttpStatus.BAD_REQUEST, ErrorCategory.VALIDATION),

    // ==================== SECURITY ERRORS (401/403) ====================
    AUTHENTICATION_FAILED("S001", "Authentication failed", HttpStatus.UNAUTHORIZED, ErrorCategory.SECURITY),
    BAD_CREDENTIALS("S002", "Invalid credentials", HttpStatus.UNAUTHORIZED, ErrorCategory.SECURITY),
    ACCESS_DENIED("S003", "Access denied", HttpStatus.FORBIDDEN, ErrorCategory.SECURITY),
    TOKEN_EXPIRED("S004", "Token has expired", HttpStatus.UNAUTHORIZED, ErrorCategory.SECURITY),
    TOKEN_INVALID("S005", "Token is invalid", HttpStatus.UNAUTHORIZED, ErrorCategory.SECURITY),
    TOKEN_MALFORMED("S006", "Token is malformed", HttpStatus.UNAUTHORIZED, ErrorCategory.SECURITY),

    // ==================== SYSTEM ERRORS (5xx) ====================
    INTERNAL_SERVER_ERROR("X001", "An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR, ErrorCategory.SYSTEM),
    DATABASE_ERROR("X002", "Database operation failed", HttpStatus.INTERNAL_SERVER_ERROR, ErrorCategory.SYSTEM),
    EXTERNAL_SERVICE_ERROR("X003", "External service error", HttpStatus.SERVICE_UNAVAILABLE, ErrorCategory.SYSTEM),
    CONCURRENT_MODIFICATION("X004", "Concurrent modification detected", HttpStatus.CONFLICT, ErrorCategory.SYSTEM);

    private final String code;
    private final String defaultMessage;
    private final HttpStatus httpStatus;
    private final ErrorCategory category;

    ErrorCode(String code, String defaultMessage, HttpStatus httpStatus, ErrorCategory category) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.httpStatus = httpStatus;
        this.category = category;
    }

    /**
     * Error taxonomy for classification and monitoring.
     */
    public enum ErrorCategory {
        BUSINESS,      // Domain logic errors (expected)
        VALIDATION,    // Input validation errors (expected)
        SECURITY,      // Auth/authorization errors (expected)
        SYSTEM         // Unexpected system errors (need investigation)
    }
}

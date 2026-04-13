package com.erandevu.exception;

import com.erandevu.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Production-grade global exception handler with:
 * - Enum-based error codes (type-safe)
 * - Reusable response builder (DRY principle)
 * - Trace ID support for observability
 * - Structured error taxonomy
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String TRACE_ID_MDC_KEY = "traceId";

    // ==================== BUSINESS EXCEPTIONS ====================

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, WebRequest request) {
        return buildErrorResponse(ex, ErrorCode.RESOURCE_NOT_FOUND, request, ex.getMessage());
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExists(
            UserAlreadyExistsException ex, WebRequest request) {
        return buildErrorResponse(ex, ErrorCode.USER_ALREADY_EXISTS, request, ex.getMessage());
    }

    @ExceptionHandler(AppointmentConflictException.class)
    public ResponseEntity<ErrorResponse> handleAppointmentConflict(
            AppointmentConflictException ex, WebRequest request) {
        Map<String, Object> details = Map.of("message", ex.getMessage());
        return buildErrorResponse(ex, ErrorCode.APPOINTMENT_CONFLICT, request, null, details);
    }

    @ExceptionHandler(InvalidAppointmentTimeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidAppointmentTime(
            InvalidAppointmentTimeException ex, WebRequest request) {
        Map<String, Object> details = Map.of("message", ex.getMessage());
        return buildErrorResponse(ex, ErrorCode.APPOINTMENT_TIME_INVALID, request, null, details);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(
            BusinessRuleException ex, WebRequest request) {
        return buildErrorResponse(ex, ErrorCode.BUSINESS_RULE_VIOLATION, request, ex.getMessage());
    }

    // ==================== SECURITY EXCEPTIONS ====================

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex, WebRequest request) {
        return buildErrorResponse(ex, ErrorCode.BAD_CREDENTIALS, request, "Invalid username or password");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(
            AuthenticationException ex, WebRequest request) {
        return buildErrorResponse(ex, ErrorCode.AUTHENTICATION_FAILED, request, "Authentication failed");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, WebRequest request) {
        return buildErrorResponse(ex, ErrorCode.ACCESS_DENIED, request, "Access denied");
    }

    // ==================== VALIDATION EXCEPTIONS ====================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            validationErrors.put(error.getField(), error.getDefaultMessage())
        );

        log.warn("Validation failed: {}", validationErrors);

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(ErrorCode.VALIDATION_ERROR.getHttpStatus().value())
                .errorCode(ErrorCode.VALIDATION_ERROR.getCode())
                .error(ErrorCode.VALIDATION_ERROR.name())
                .message(ErrorCode.VALIDATION_ERROR.getDefaultMessage())
                .path(request.getDescription(false))
                .traceId(getTraceId())
                .validationErrors(validationErrors)
                .build();

        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.getHttpStatus()).body(response);
    }

    // ==================== FALLBACK HANDLER ====================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex, WebRequest request) {
        log.error("Unexpected system error occurred", ex);

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus().value())
                .errorCode(ErrorCode.INTERNAL_SERVER_ERROR.getCode())
                .error(ErrorCode.INTERNAL_SERVER_ERROR.name())
                .message(ErrorCode.INTERNAL_SERVER_ERROR.getDefaultMessage())
                .path(request.getDescription(false))
                .traceId(getTraceId())
                .build();

        return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus()).body(response);
    }

    // ==================== REUSABLE BUILDER METHODS ====================

    /**
     * Build error response with error code and custom message.
     */
    private ResponseEntity<ErrorResponse> buildErrorResponse(
            Exception ex, ErrorCode errorCode, WebRequest request, String customMessage) {
        return buildErrorResponse(ex, errorCode, request, customMessage, null);
    }

    /**
     * Build error response with all optional fields.
     */
    private ResponseEntity<ErrorResponse> buildErrorResponse(
            Exception ex, ErrorCode errorCode, WebRequest request,
            String customMessage, Map<String, Object> details) {

        String message = customMessage != null ? customMessage : errorCode.getDefaultMessage();

        // Log with appropriate level based on error category
        logByCategory(ex, errorCode, message);

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(errorCode.getHttpStatus().value())
                .errorCode(errorCode.getCode())
                .error(errorCode.name())
                .message(message)
                .path(request.getDescription(false))
                .traceId(getTraceId())
                .details(details)
                .build();

        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }

    /**
     * Log with appropriate level based on error taxonomy.
     */
    private void logByCategory(Exception ex, ErrorCode errorCode, String message) {
        String logMessage = String.format("[%s] %s: %s", errorCode.getCode(), errorCode.name(), message);

        switch (errorCode.getCategory()) {
            case SYSTEM -> log.error(logMessage, ex);
            case SECURITY -> log.warn("Security event - {}", logMessage);
            case VALIDATION -> log.debug(logMessage);
            default -> log.warn(logMessage);
        }
    }

    /**
     * Get or generate trace ID for request correlation.
     */
    private String getTraceId() {
        String traceId = MDC.get(TRACE_ID_MDC_KEY);
        if (traceId == null) {
            traceId = UUID.randomUUID().toString();
            MDC.put(TRACE_ID_MDC_KEY, traceId);
        }
        return traceId;
    }
}

package com.erandevu.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ConcurrentBookingException.class)
    public ResponseEntity<ErrorResponse> handleConcurrentBookingException(
            ConcurrentBookingException ex, WebRequest request) {
        
        log.warn("Concurrent booking detected: {}", ex.getMessage());
        
        Map<String, Object> conflictDetails = new HashMap<>();
        if (ex.getDoctorId() != null) {
            conflictDetails.put("doctorId", ex.getDoctorId());
        }
        if (ex.getAppointmentDateTime() != null) {
            conflictDetails.put("appointmentDateTime", ex.getAppointmentDateTime());
        }
        if (ex.getConflictingAppointmentId() != null) {
            conflictDetails.put("conflictingAppointmentId", ex.getConflictingAppointmentId());
        }
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("CONCURRENT_BOOKING")
                .message("Appointment slot is currently being booked by another user. Please try again.")
                .path(request.getDescription(false))
                .details(conflictDetails)
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLockException(
            OptimisticLockException ex, WebRequest request) {
        
        log.warn("Optimistic lock failure: {}", ex.getMessage());
        
        Map<String, Object> lockDetails = new HashMap<>();
        if (ex.getEntityType() != null) {
            lockDetails.put("entityType", ex.getEntityType());
        }
        if (ex.getEntityId() != null) {
            lockDetails.put("entityId", ex.getEntityId());
        }
        if (ex.getCurrentVersion() != null) {
            lockDetails.put("currentVersion", ex.getCurrentVersion());
        }
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("OPTIMISTIC_LOCK_FAILURE")
                .message("The data was modified by another user. Please refresh and try again.")
                .path(request.getDescription(false))
                .details(lockDetails)
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleSpringOptimisticLockException(
            org.springframework.orm.ObjectOptimisticLockingFailureException ex, WebRequest request) {
        
        log.warn("Spring optimistic lock failure: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("OPTIMISTIC_LOCK_FAILURE")
                .message("The data was modified by another user. Please refresh and try again.")
                .path(request.getDescription(false))
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
            org.springframework.dao.DataIntegrityViolationException ex, WebRequest request) {
        
        log.warn("Data integrity violation: {}", ex.getMessage());
        
        String message = "Data constraint violation occurred";
        if (ex.getMessage() != null && ex.getMessage().contains("appointments_doctor_id_appointment_datetime_key")) {
            message = "This appointment slot is already booked. Please choose a different time.";
        }
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("DATA_INTEGRITY_VIOLATION")
                .message(message)
                .path(request.getDescription(false))
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRuleException(
            BusinessRuleException ex, WebRequest request) {
        
        log.warn("Business rule violation: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("BUSINESS_RULE_VIOLATION")
                .message(ex.getMessage())
                .path(request.getDescription(false))
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        
        log.warn("Resource not found: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("RESOURCE_NOT_FOUND")
                .message(ex.getMessage())
                .path(request.getDescription(false))
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(AppointmentConflictException.class)
    public ResponseEntity<ErrorResponse> handleAppointmentConflictException(
            AppointmentConflictException ex, WebRequest request) {
        
        log.warn("Appointment conflict: {}", ex.getMessage());
        
        Map<String, Object> conflictDetails = new HashMap<>();
        if (ex.getDoctorId() != null) {
            conflictDetails.put("doctorId", ex.getDoctorId());
        }
        if (ex.getAppointmentDateTime() != null) {
            conflictDetails.put("appointmentDateTime", ex.getAppointmentDateTime());
        }
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("APPOINTMENT_CONFLICT")
                .message(ex.getMessage())
                .path(request.getDescription(false))
                .details(conflictDetails)
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExistsException(
            UserAlreadyExistsException ex, WebRequest request) {
        
        log.warn("User already exists: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("USER_ALREADY_EXISTS")
                .message(ex.getMessage())
                .path(request.getDescription(false))
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InvalidAppointmentTimeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidAppointmentTimeException(
            InvalidAppointmentTimeException ex, WebRequest request) {
        
        log.warn("Invalid appointment time: {}", ex.getMessage());
        
        Map<String, Object> timeViolationDetails = new HashMap<>();
        if (ex.getAppointmentDateTime() != null) {
            timeViolationDetails.put("appointmentDateTime", ex.getAppointmentDateTime());
        }
        if (ex.getViolationType() != null) {
            timeViolationDetails.put("violationType", ex.getViolationType());
        }
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("INVALID_APPOINTMENT_TIME")
                .message(ex.getMessage())
                .path(request.getDescription(false))
                .details(timeViolationDetails)
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(
            BadCredentialsException ex, WebRequest request) {
        
        log.warn("Bad credentials attempt");
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("AUTHENTICATION_FAILED")
                .message("Invalid username or password")
                .path(request.getDescription(false))
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex, WebRequest request) {
        
        log.warn("Authentication failed: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("AUTHENTICATION_FAILED")
                .message(ex.getMessage())
                .path(request.getDescription(false))
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        
        log.warn("Access denied: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error("ACCESS_DENIED")
                .message("You don't have permission to access this resource")
                .path(request.getDescription(false))
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        log.warn("Validation failed: {}", errors);
        
        ValidationErrorResponse errorResponse = ValidationErrorResponse.validationBuilder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("VALIDATION_ERROR")
                .message("Request validation failed")
                .validationErrors(errors)
                .path(request.getDescription(false))
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex, WebRequest request) {
        
        log.error("Unexpected error occurred", ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("INTERNAL_SERVER_ERROR")
                .message("An unexpected error occurred")
                .path(request.getDescription(false))
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Error response payload")
    public static class ErrorResponse {
        @Schema(description = "Timestamp of error", example = "2024-01-01T10:00:00")
        public LocalDateTime timestamp;

        @Schema(description = "HTTP status code", example = "404")
        public Integer status;

        @Schema(description = "Error type", example = "RESOURCE_NOT_FOUND")
        public String error;

        @Schema(description = "Error message", example = "User not found with id: 1")
        public String message;

        @Schema(description = "Request path", example = "/api/users/1")
        public String path;

        @Schema(description = "Additional error details")
        public Map<String, Object> details;
        
        public static ErrorResponseBuilder builder() {
            return new ErrorResponseBuilder();
        }
        
        public static class ErrorResponseBuilder {
            private LocalDateTime timestamp;
            private Integer status;
            private String error;
            private String message;
            private String path;
            private Map<String, Object> details;
            
            public ErrorResponseBuilder timestamp(LocalDateTime timestamp) {
                this.timestamp = timestamp;
                return this;
            }
            
            public ErrorResponseBuilder status(Integer status) {
                this.status = status;
                return this;
            }
            
            public ErrorResponseBuilder error(String error) {
                this.error = error;
                return this;
            }
            
            public ErrorResponseBuilder message(String message) {
                this.message = message;
                return this;
            }
            
            public ErrorResponseBuilder path(String path) {
                this.path = path;
                return this;
            }
            
            public ErrorResponseBuilder details(Map<String, Object> details) {
                this.details = details;
                return this;
            }
            
            public ErrorResponse build() {
                ErrorResponse response = new ErrorResponse();
                response.timestamp = this.timestamp;
                response.status = this.status;
                response.error = this.error;
                response.message = this.message;
                response.path = this.path;
                response.details = this.details;
                return response;
            }
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Validation error response payload")
    @EqualsAndHashCode(callSuper = false)
    public static class ValidationErrorResponse extends ErrorResponse {
        @Schema(description = "Field validation errors")
        public Map<String, String> validationErrors;
        
        public static ValidationErrorResponseBuilder validationBuilder() {
            return new ValidationErrorResponseBuilder();
        }
        
        public static class ValidationErrorResponseBuilder {
            private LocalDateTime timestamp;
            private Integer status;
            private String error;
            private String message;
            private String path;
            private Map<String, String> validationErrors;
            
            public ValidationErrorResponseBuilder timestamp(LocalDateTime timestamp) {
                this.timestamp = timestamp;
                return this;
            }
            
            public ValidationErrorResponseBuilder status(Integer status) {
                this.status = status;
                return this;
            }
            
            public ValidationErrorResponseBuilder error(String error) {
                this.error = error;
                return this;
            }
            
            public ValidationErrorResponseBuilder message(String message) {
                this.message = message;
                return this;
            }
            
            public ValidationErrorResponseBuilder path(String path) {
                this.path = path;
                return this;
            }
            
            public ValidationErrorResponseBuilder validationErrors(Map<String, String> validationErrors) {
                this.validationErrors = validationErrors;
                return this;
            }
            
            public ValidationErrorResponse build() {
                ValidationErrorResponse response = new ValidationErrorResponse();
                response.timestamp = this.timestamp;
                response.status = this.status;
                response.error = this.error;
                response.message = this.message;
                response.path = this.path;
                response.validationErrors = this.validationErrors;
                return response;
            }
        }
    }
}

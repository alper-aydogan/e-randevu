package com.erandevu.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
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
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Resource Not Found")
                .message(ex.getMessage())
                .path(request.getDescription(false))
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(AppointmentConflictException.class)
    public ResponseEntity<ErrorResponse> handleAppointmentConflictException(
            AppointmentConflictException ex, WebRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("Appointment Conflict")
                .message(ex.getMessage())
                .path(request.getDescription(false))
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExistsException(
            UserAlreadyExistsException ex, WebRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("User Already Exists")
                .message(ex.getMessage())
                .path(request.getDescription(false))
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InvalidAppointmentTimeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidAppointmentTimeException(
            InvalidAppointmentTimeException ex, WebRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Invalid Appointment Time")
                .message(ex.getMessage())
                .path(request.getDescription(false))
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(
            BadCredentialsException ex, WebRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Authentication Failed")
                .message("Invalid username or password")
                .path(request.getDescription(false))
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex, WebRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Authentication Failed")
                .message(ex.getMessage())
                .path(request.getDescription(false))
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error("Access Denied")
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
        
        ValidationErrorResponse errorResponse = ValidationErrorResponse.validationBuilder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Request validation failed")
                .validationErrors(errors)
                .path(request.getDescription(false))
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex, WebRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
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

        @Schema(description = "Error type", example = "Resource Not Found")
        public String error;

        @Schema(description = "Error message", example = "User not found with id: 1")
        public String message;

        @Schema(description = "Request path", example = "/api/users/1")
        public String path;
        
        public static ErrorResponseBuilder builder() {
            return new ErrorResponseBuilder();
        }
        
        public static class ErrorResponseBuilder {
            private LocalDateTime timestamp;
            private Integer status;
            private String error;
            private String message;
            private String path;
            
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
            
            public ErrorResponse build() {
                ErrorResponse response = new ErrorResponse();
                response.timestamp = this.timestamp;
                response.status = this.status;
                response.error = this.error;
                response.message = this.message;
                response.path = this.path;
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

package com.erandevu.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Production-grade immutable error response for API errors.
 * Includes traceId for distributed tracing and observability.
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "Error response for API errors with trace support")
public class ErrorResponse {

    @Schema(description = "Timestamp of the error in ISO format", example = "2024-01-01T10:00:00")
    private LocalDateTime timestamp;

    @Schema(description = "HTTP status code", example = "404")
    private Integer status;

    @Schema(description = "Machine-readable error code (e.g., E001, S001)", example = "E001")
    private String errorCode;

    @Schema(description = "Human-readable error type", example = "RESOURCE_NOT_FOUND")
    private String error;

    @Schema(description = "Error message", example = "User not found with id: 1")
    private String message;

    @Schema(description = "Request path that caused the error", example = "/api/users/1")
    private String path;

    @Schema(description = "Unique trace ID for request tracking across services", example = "550e8400-e29b-41d4-a716-446655440000")
    private String traceId;

    @Schema(description = "Field-level validation errors (key=field, value=message)")
    private Map<String, String> validationErrors;

    @Schema(description = "Additional structured error details")
    private Map<String, Object> details;
}

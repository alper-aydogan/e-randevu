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
 * Immutable error response for API errors.
 * All fields are private - use builder pattern for creation.
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "Error response for API errors")
public class ErrorResponse {

    @Schema(description = "Timestamp of the error", example = "2024-01-01T10:00:00")
    private LocalDateTime timestamp;

    @Schema(description = "HTTP status code", example = "404")
    private Integer status;

    @Schema(description = "Error type", example = "RESOURCE_NOT_FOUND")
    private String error;

    @Schema(description = "Error message", example = "User not found with id: 1")
    private String message;

    @Schema(description = "Request path", example = "/api/users/1")
    private String path;

    @Schema(description = "Validation errors for field-level validation")
    private Map<String, String> validationErrors;

    @Schema(description = "Additional error details")
    private Map<String, Object> details;
}

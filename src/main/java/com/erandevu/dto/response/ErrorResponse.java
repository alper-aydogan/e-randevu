package com.erandevu.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Error response for API errors")
public class ErrorResponse {

    @Schema(description = "Timestamp of the error", example = "2024-01-01T10:00:00")
    public LocalDateTime timestamp;

    @Schema(description = "HTTP status code", example = "404")
    public Integer status;

    @Schema(description = "Error type", example = "RESOURCE_NOT_FOUND")
    public String error;

    @Schema(description = "Error message", example = "User not found with id: 1")
    public String message;

    @Schema(description = "Request path", example = "/api/users/1")
    public String path;

    @Schema(description = "Validation errors for field-level validation")
    public Map<String, String> validationErrors;

    @Schema(description = "Additional error details")
    public Map<String, Object> details;
}

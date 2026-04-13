package com.erandevu.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Base response DTO containing audit fields.
 * All entity response DTOs should extend this class.
 * Provides consistent audit information across all API responses.
 */
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseAuditResponse {

    @Schema(description = "Creation timestamp", example = "2024-01-01T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp", example = "2024-01-01T10:00:00")
    private LocalDateTime updatedAt;

    @Schema(description = "Created by user", example = "admin")
    private String createdBy;

    @Schema(description = "Last updated by user", example = "admin")
    private String updatedBy;
}

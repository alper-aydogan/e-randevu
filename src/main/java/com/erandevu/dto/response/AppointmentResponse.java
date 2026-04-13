package com.erandevu.dto.response;

import com.erandevu.enums.AppointmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Immutable appointment response.
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "Appointment response")
public class AppointmentResponse {
    
    @Schema(description = "Appointment ID", example = "1")
    private Long id;
    
    @Schema(description = "Doctor ID", example = "1")
    private Long doctorId;
    
    @Schema(description = "Doctor name", example = "Dr. John Smith")
    private String doctorName;
    
    @Schema(description = "Patient ID", example = "2")
    private Long patientId;
    
    @Schema(description = "Patient name", example = "Jane Doe")
    private String patientName;
    
    @Schema(description = "Appointment date and time", example = "2024-12-25T10:30:00")
    private LocalDateTime appointmentDateTime;
    
    @Schema(description = "Appointment end time", example = "2024-12-25T11:00:00")
    private LocalDateTime endDateTime;
    
    @Schema(description = "Appointment notes", example = "Regular checkup")
    private String notes;
    
    @Schema(description = "Cancellation reason", example = "Patient requested")
    private String cancellationReason;
    
    @Schema(description = "Appointment status", example = "SCHEDULED")
    private AppointmentStatus status;
    
    @Schema(description = "Creation date", example = "2024-01-01T10:00:00")
    private LocalDateTime createdAt;
    
    @Schema(description = "Last update date", example = "2024-01-01T10:00:00")
    private LocalDateTime updatedAt;
    
    @Schema(description = "Created by", example = "admin")
    private String createdBy;
    
    @Schema(description = "Updated by", example = "admin")
    private String updatedBy;
}

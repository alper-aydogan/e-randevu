package com.erandevu.dto.response;

import com.erandevu.enums.AppointmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Immutable appointment response extending base audit fields.
 * Uses inheritance for consistent audit information across all entity responses.
 */
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Schema(description = "Appointment response with audit information")
public class AppointmentResponse extends BaseAuditResponse {

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
}

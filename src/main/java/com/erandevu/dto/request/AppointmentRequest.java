package com.erandevu.dto.request;

import com.erandevu.dto.validation.ValidationMessages;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Appointment creation request DTO.
 * SECURITY: Patient ID is NOT included - derived from authenticated JWT token.
 * Prevents IDOR (Insecure Direct Object Reference) attacks.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Appointment creation request. Patient is derived from authenticated user token.")
public class AppointmentRequest {

    @NotNull(message = ValidationMessages.DOCTOR_ID_REQUIRED)
    @Positive(message = ValidationMessages.DOCTOR_ID_POSITIVE)
    @Schema(description = "ID of the doctor to book appointment with", example = "1", required = true)
    private Long doctorId;

    // ❌ REMOVED: private Long patientId - SECURITY RISK! IDOR attack possible
    // Patient ID must be extracted from JWT token in controller/service layer

    @NotNull(message = ValidationMessages.APPOINTMENT_DATETIME_REQUIRED)
    @Future(message = ValidationMessages.APPOINTMENT_DATETIME_FUTURE)
    @Schema(description = "Desired appointment date and time (must be in future, business hours only)",
            example = "2024-12-25T10:30:00",
            required = true)
    private LocalDateTime appointmentDateTime;

    @Size(max = 500, message = ValidationMessages.NOTES_SIZE)
    @Schema(description = "Optional notes for the doctor", example = "Regular checkup for annual physical examination")
    private String notes;

    // Manual getters for Lombok workaround
    public Long getDoctorId() {
        return doctorId;
    }

    public LocalDateTime getAppointmentDateTime() {
        return appointmentDateTime;
    }

    public String getNotes() {
        return notes;
    }
}

package com.erandevu.dto.request;

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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Appointment creation request")
public class AppointmentRequest {
    
    @NotNull(message = "Doctor ID is required")
    @Positive(message = "Doctor ID must be positive")
    @Schema(example = "1", description = "ID of the doctor for the appointment")
    private Long doctorId;
    
    @NotNull(message = "Patient ID is required")
    @Positive(message = "Patient ID must be positive")
    @Schema(example = "2", description = "ID of the patient for the appointment")
    private Long patientId;
    
    @NotNull(message = "Appointment date and time is required")
    @Future(message = "Appointment date and time must be in the future")
    @Schema(example = "2024-12-25T10:30:00", description = "Scheduled date and time for the appointment")
    private LocalDateTime appointmentDateTime;
    
    @Size(max = 500, message = "Notes must not exceed 500 characters")
    @Schema(example = "Regular checkup for annual physical examination", description = "Additional notes about the appointment")
    private String notes;
}

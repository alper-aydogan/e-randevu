package com.erandevu.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
    @Schema(example = "1")
    private Long doctorId;
    
    @NotNull(message = "Patient ID is required")
    @Positive(message = "Patient ID must be positive")
    @Schema(example = "2")
    private Long patientId;
    
    @NotNull(message = "Appointment date and time is required")
    @Future(message = "Appointment date and time must be in the future")
    @Schema(example = "2024-12-25T10:30:00")
    private LocalDateTime appointmentDateTime;
    
    @Schema(example = "Regular checkup")
    private String notes;
}

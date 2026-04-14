package com.erandevu.application.appointment;

import java.time.LocalDateTime;

/**
 * Command object for CreateAppointmentUseCase.
 * Clean Architecture: Input DTO for the use case.
 */
public record CreateAppointmentCommand(
    Long doctorId,
    Long patientId,
    LocalDateTime appointmentDateTime,
    String notes
) {
    /**
     * Factory method with validation.
     */
    public static CreateAppointmentCommand of(
            Long doctorId,
            Long patientId,
            LocalDateTime appointmentDateTime,
            String notes) {
        if (doctorId == null || doctorId <= 0) {
            throw new IllegalArgumentException("Doctor ID is required");
        }
        if (patientId == null || patientId <= 0) {
            throw new IllegalArgumentException("Patient ID is required");
        }
        if (appointmentDateTime == null) {
            throw new IllegalArgumentException("Appointment date/time is required");
        }
        if (appointmentDateTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Appointment must be in the future");
        }
        return new CreateAppointmentCommand(doctorId, patientId, appointmentDateTime, notes);
    }
}

package com.erandevu.service.validation;

import com.erandevu.entity.Appointment;
import com.erandevu.exception.AppointmentConflictException;
import com.erandevu.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Single Responsibility: Validates appointment conflicts (overlapping appointments).
 * Uses pessimistic locking at repository level.
 */
@Component
@RequiredArgsConstructor
public class AppointmentConflictValidator {

    private final AppointmentRepository appointmentRepository;
    public static final int DEFAULT_APPOINTMENT_DURATION_MINUTES = 30;

    public void validate(Long doctorId, LocalDateTime appointmentDateTime) {
        validate(doctorId, appointmentDateTime, null);
    }

    public void validate(Long doctorId, LocalDateTime appointmentDateTime, Long excludeAppointmentId) {
        LocalDateTime appointmentEnd = appointmentDateTime.plusMinutes(DEFAULT_APPOINTMENT_DURATION_MINUTES);

        boolean hasConflicts = appointmentRepository.hasOverlappingAppointments(
            doctorId, appointmentDateTime, appointmentEnd
        );

        if (hasConflicts) {
            List<Appointment> conflicts = appointmentRepository.findOverlappingAppointments(
                doctorId, appointmentDateTime, appointmentEnd
            );

            boolean realConflict = conflicts.stream()
                .anyMatch(a -> !a.getId().equals(excludeAppointmentId));

            if (realConflict) {
                throw new AppointmentConflictException(
                    "Doctor already has an appointment at this time: " + appointmentDateTime,
                    doctorId,
                    appointmentDateTime
                );
            }
        }
    }
}

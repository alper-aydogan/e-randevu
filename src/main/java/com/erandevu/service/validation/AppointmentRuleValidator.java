package com.erandevu.service.validation;

import com.erandevu.entity.Appointment;
import com.erandevu.enums.AppointmentStatus;
import com.erandevu.exception.BusinessRuleException;
import com.erandevu.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Single Responsibility: Validates business rules for appointments.
 * Checks: daily limits, doctor≠patient, cancellation rules, status transitions.
 */
@Component
@RequiredArgsConstructor
public class AppointmentRuleValidator {

    private final AppointmentRepository appointmentRepository;
    public static final int MAX_APPOINTMENTS_PER_DOCTOR_PER_DAY = 8;
    public static final int MIN_HOURS_BEFORE_CANCELLATION = 2;

    public void validateDoctorIsNotPatient(Long doctorId, Long patientId) {
        if (doctorId.equals(patientId)) {
            throw new BusinessRuleException("Doctor and patient cannot be the same person");
        }
    }

    public void validateDailyAppointmentLimit(Long doctorId, LocalDateTime appointmentDateTime) {
        LocalDate appointmentDate = appointmentDateTime.toLocalDate();
        LocalDateTime startOfDay = appointmentDate.atStartOfDay();

        long dailyCount = appointmentRepository.countDailyAppointments(doctorId, startOfDay);

        if (dailyCount >= MAX_APPOINTMENTS_PER_DOCTOR_PER_DAY) {
            throw new BusinessRuleException(
                "Doctor has reached maximum daily appointment limit (" + MAX_APPOINTMENTS_PER_DOCTOR_PER_DAY + ")"
            );
        }
    }

    public void validateCanBeModified(Appointment appointment) {
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new BusinessRuleException(
                "Only scheduled appointments can be modified. Current status: " + appointment.getStatus()
            );
        }
    }

    public void validateCancellationRules(Appointment appointment) {
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new BusinessRuleException("Appointment is already cancelled");
        }

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new BusinessRuleException("Cannot cancel completed appointment");
        }

        LocalDateTime appointmentTime = appointment.getAppointmentDateTime();
        if (appointmentTime.isBefore(LocalDateTime.now().plusHours(MIN_HOURS_BEFORE_CANCELLATION))) {
            throw new BusinessRuleException(
                "Cannot cancel appointments less than " + MIN_HOURS_BEFORE_CANCELLATION + " hours before scheduled time"
            );
        }
    }
}

package com.erandevu.service.validation;

import com.erandevu.dto.request.AppointmentRequest;
import com.erandevu.entity.Appointment;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Composite validator that orchestrates all appointment validation rules.
 * This is the entry point for all appointment validations.
 */
@Component
@RequiredArgsConstructor
public class AppointmentValidator {

    private static final Logger log = LoggerFactory.getLogger(AppointmentValidator.class);

    private final AppointmentTimeValidator timeValidator;
    private final AppointmentConflictValidator conflictValidator;
    private final DoctorAvailabilityValidator availabilityValidator;
    private final AppointmentRuleValidator ruleValidator;

    /**
     * Validates all rules for creating a new appointment.
     */
    public void validateCreation(AppointmentRequest request, Long doctorId, Long patientId) {
        log.debug("Validating appointment creation: doctor={}, patient={}, time={}",
            doctorId, patientId, request.getAppointmentDateTime());

        // 1. Time constraints
        timeValidator.validate(request.getAppointmentDateTime());

        // 2. Business rules
        ruleValidator.validateDoctorIsNotPatient(doctorId, patientId);
        ruleValidator.validateDailyAppointmentLimit(doctorId, request.getAppointmentDateTime());

        // 3. Doctor availability
        availabilityValidator.validate(doctorId, request.getAppointmentDateTime());

        // 4. Conflict check (uses pessimistic locking in repository)
        conflictValidator.validate(doctorId, request.getAppointmentDateTime());

        log.debug("Appointment validation passed for doctor={}", doctorId);
    }

    /**
     * Validates all rules for updating an existing appointment.
     */
    public void validateUpdate(Appointment existing, AppointmentRequest request) {
        log.debug("Validating appointment update: id={}", existing.getId());

        // 1. Can be modified
        ruleValidator.validateCanBeModified(existing);

        // 2. Time constraints
        timeValidator.validate(request.getAppointmentDateTime());

        // 3. Doctor availability
        availabilityValidator.validate(request.getDoctorId(), request.getAppointmentDateTime());

        // 4. Conflict check (excluding current appointment)
        conflictValidator.validate(request.getDoctorId(), request.getAppointmentDateTime(), existing.getId());

        // 5. Daily limit
        ruleValidator.validateDailyAppointmentLimit(request.getDoctorId(), request.getAppointmentDateTime());

        log.debug("Appointment update validation passed for id={}", existing.getId());
    }

    /**
     * Validates cancellation rules.
     */
    public void validateCancellation(Appointment appointment) {
        log.debug("Validating appointment cancellation: id={}", appointment.getId());
        ruleValidator.validateCancellationRules(appointment);
    }
}

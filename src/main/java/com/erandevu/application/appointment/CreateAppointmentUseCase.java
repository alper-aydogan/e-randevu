package com.erandevu.application.appointment;

import com.erandevu.application.UseCase;
import com.erandevu.domain.event.AppointmentCreatedEvent;
import com.erandevu.dto.response.AppointmentResponse;
import com.erandevu.entity.Appointment;
import com.erandevu.entity.User;
import com.erandevu.enums.AppointmentStatus;
import com.erandevu.exception.AppointmentConflictException;
import com.erandevu.exception.InvalidAppointmentTimeException;
import com.erandevu.exception.ResourceNotFoundException;
import com.erandevu.mapper.AppointmentMapper;
import com.erandevu.repository.AppointmentRepository;
import com.erandevu.repository.UserRepository;
import com.erandevu.service.validation.AppointmentConflictValidator;
import com.erandevu.service.validation.DoctorAvailabilityValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Application Use Case: Create Appointment.
 * Orchestrates the creation of a new appointment with validation and event publishing.
 * Implements Clean Architecture - all business logic is here, not in Service layer.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CreateAppointmentUseCase implements UseCase<CreateAppointmentCommand, AppointmentResponse> {

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final AppointmentConflictValidator conflictValidator;
    private final DoctorAvailabilityValidator availabilityValidator;
    private final AppointmentMapper appointmentMapper;
    private final ApplicationEventPublisher eventPublisher;

    // Business rule constants
    private static final int MIN_ADVANCE_BOOKING_HOURS = 2;
    private static final int MAX_BOOKING_DAYS_AHEAD = 30;
    private static final LocalTime BUSINESS_START = LocalTime.of(9, 0);
    private static final LocalTime BUSINESS_END = LocalTime.of(18, 0);

    @Override
    public AppointmentResponse execute(CreateAppointmentCommand command) {
        log.info("UC: Creating appointment - doctor={}, patient={}",
            command.doctorId(), command.patientId());

        try {
            // 1. Validate business rules (inlined from validators for Clean Architecture)
            validateBusinessRules(command);

            // 2. Check doctor availability
            availabilityValidator.validate(command.doctorId(), command.appointmentDateTime());

            // 3. Check appointment conflicts
            conflictValidator.validate(command.doctorId(), command.appointmentDateTime());

            // 4. Fetch entities
            User doctor = userRepository.findById(command.doctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + command.doctorId()));
            User patient = userRepository.findById(command.patientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + command.patientId()));

            // 5. Create domain entity (domain logic handles its own state)
            Appointment appointment = Appointment.builder()
                .doctor(doctor)
                .patient(patient)
                .appointmentDateTime(command.appointmentDateTime())
                .notes(command.notes())
                .status(AppointmentStatus.SCHEDULED)
                .build();

            // 6. Persist
            Appointment saved = appointmentRepository.save(appointment);
            log.info("UC: Appointment created - id={}", saved.getId());

            // 7. Publish domain event
            eventPublisher.publishEvent(new AppointmentCreatedEvent(saved));

            // 8. Return response
            return appointmentMapper.toAppointmentResponse(saved);

        } catch (DataIntegrityViolationException | ObjectOptimisticLockingFailureException e) {
            log.warn("UC: Concurrent booking conflict - doctor={}, time={}",
                command.doctorId(), command.appointmentDateTime());
            throw new AppointmentConflictException(
                "Slot already booked", command.doctorId(), command.appointmentDateTime());
        }
    }

    /**
     * Validates all business rules for appointment creation.
     * Business rules that don't require external state are validated here.
     */
    private void validateBusinessRules(CreateAppointmentCommand command) {
        LocalDateTime appointmentDateTime = command.appointmentDateTime();

        // Rule 1: Not in past
        if (appointmentDateTime.isBefore(LocalDateTime.now())) {
            throw new InvalidAppointmentTimeException("Appointment time must be in the future");
        }

        // Rule 2: Advance booking
        if (appointmentDateTime.isBefore(LocalDateTime.now().plusHours(MIN_ADVANCE_BOOKING_HOURS))) {
            throw new InvalidAppointmentTimeException(
                "Appointments must be booked at least " + MIN_ADVANCE_BOOKING_HOURS + " hours in advance");
        }

        // Rule 3: Max future booking
        if (appointmentDateTime.isAfter(LocalDateTime.now().plusDays(MAX_BOOKING_DAYS_AHEAD))) {
            throw new InvalidAppointmentTimeException(
                "Appointments cannot be booked more than " + MAX_BOOKING_DAYS_AHEAD + " days in advance");
        }

        // Rule 4: Weekday only
        DayOfWeek dayOfWeek = appointmentDateTime.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            throw new InvalidAppointmentTimeException("Appointments are not available on weekends");
        }

        // Rule 5: Business hours
        LocalTime time = appointmentDateTime.toLocalTime();
        if (time.isBefore(BUSINESS_START) || time.isAfter(BUSINESS_END)) {
            throw new InvalidAppointmentTimeException(
                "Appointments are only available between " + BUSINESS_START + " and " + BUSINESS_END);
        }

        // Rule 6: Doctor cannot be patient
        if (command.doctorId().equals(command.patientId())) {
            throw new InvalidAppointmentTimeException("Doctor cannot book appointment with themselves");
        }

        log.debug("Appointment validation passed for doctor={}", command.doctorId());
    }
}

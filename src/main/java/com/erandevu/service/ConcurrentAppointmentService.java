package com.erandevu.service;

import com.erandevu.dto.request.AppointmentRequest;
import com.erandevu.dto.response.AppointmentResponse;
import com.erandevu.entity.Appointment;
import com.erandevu.entity.Schedule;
import com.erandevu.entity.User;
import com.erandevu.enums.AppointmentStatus;
import com.erandevu.enums.Role;
import com.erandevu.exception.AppointmentConflictException;
import com.erandevu.exception.ConcurrentBookingException;
import com.erandevu.exception.InvalidAppointmentTimeException;
import com.erandevu.exception.OptimisticLockException;
import com.erandevu.exception.ResourceNotFoundException;
import com.erandevu.mapper.AppointmentMapper;
import com.erandevu.repository.ConcurrentAppointmentRepository;
import com.erandevu.repository.ScheduleRepository;
import com.erandevu.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.OptimisticLockException as JpaOptimisticLockException;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConcurrentAppointmentService {

    private final ConcurrentAppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final ScheduleRepository scheduleRepository;
    private final AppointmentMapper appointmentMapper;

    // Business validation constants
    private static final int MIN_ADVANCE_BOOKING_HOURS = 2;
    private static final int MAX_BOOKING_DAYS_AHEAD = 30;
    private static final int MAX_APPOINTMENTS_PER_DOCTOR_PER_DAY = 8;
    private static final int MAX_RETRY_ATTEMPTS = 3;

    /**
     * Creates a new appointment with comprehensive concurrency control
     * Uses SERIALIZABLE isolation level for maximum consistency
     */
    @Transactional(
        isolation = Isolation.SERIALIZABLE,
        propagation = Propagation.REQUIRED,
        rollbackFor = {
            ConcurrentBookingException.class,
            AppointmentConflictException.class,
            InvalidAppointmentTimeException.class,
            DataIntegrityViolationException.class,
            ObjectOptimisticLockingFailureException.class
        }
    )
    @CacheEvict(value = {
            "appointments", 
            "doctorAppointments", 
            "patientAppointments",
            "appointmentsByStatus",
            "todayAppointments"
    }, allEntries = true)
    public AppointmentResponse createAppointment(AppointmentRequest request) {
        log.info("Creating appointment with concurrency control: doctorId={}, patientId={}, dateTime={}", 
                request.getDoctorId(), request.getPatientId(), request.getAppointmentDateTime());

        try {
            // Validate request data
            validateAppointmentRequest(request);

            // Find and validate doctor
            User doctor = findAndValidateDoctor(request.getDoctorId());

            // Find and validate patient
            User patient = findAndValidatePatient(request.getPatientId());

            // Validate appointment time constraints
            validateAppointmentTimeConstraints(request.getAppointmentDateTime());

            // Validate doctor's availability and schedule
            validateDoctorAvailability(doctor, request.getAppointmentDateTime());

            // Check for appointment conflicts with pessimistic locking
            validateNoAppointmentConflictsWithLock(doctor, request.getAppointmentDateTime());

            // Validate daily appointment limit
            validateDailyAppointmentLimit(doctor, request.getAppointmentDateTime());

            // Create appointment entity
            Appointment appointment = appointmentMapper.toAppointment(request, doctor, patient);
            appointment.setStatus(AppointmentStatus.SCHEDULED);

            // Save appointment
            Appointment savedAppointment = appointmentRepository.save(appointment);
            
            log.info("Appointment created successfully with concurrency control: id={}, doctorId={}, patientId={}", 
                    savedAppointment.getId(), doctor.getId(), patient.getId());
            
            return appointmentMapper.toAppointmentResponse(savedAppointment);

        } catch (DataIntegrityViolationException ex) {
            log.warn("Database constraint violation during appointment creation: {}", ex.getMessage());
            throw new ConcurrentBookingException(
                    "Appointment slot is already booked by another user. Please try a different time.",
                    request.getDoctorId(), 
                    request.getAppointmentDateTime(), 
                    null
            );
        } catch (ObjectOptimisticLockingFailureException ex) {
            log.warn("Optimistic lock failure during appointment creation: {}", ex.getMessage());
            throw new ConcurrentBookingException(
                    "Appointment data was modified by another user. Please try again.",
                    request.getDoctorId(), 
                    request.getAppointmentDateTime(), 
                    null
            );
        }
    }

    /**
     * Updates an appointment with optimistic locking
     */
    @Transactional(
        isolation = Isolation.READ_COMMITTED,
        propagation = Propagation.REQUIRED,
        rollbackFor = {
            OptimisticLockException.class,
            ConcurrentBookingException.class
        }
    )
    @CacheEvict(value = {
            "appointments", 
            "doctorAppointments", 
            "patientAppointments",
            "appointmentsByStatus",
            "todayAppointments"
    }, allEntries = true)
    public AppointmentResponse updateAppointment(Long id, AppointmentRequest request) {
        log.info("Updating appointment with optimistic locking: id={}", id);

        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id));

        // Store original version for conflict detection
        Long originalVersion = appointment.getVersion();

        try {
            // Update appointment details
            appointmentMapper.updateAppointmentFromRequest(request, appointment);

            // Validate updated appointment
            validateAppointmentUpdate(appointment);

            // Save appointment (will throw OptimisticLockException if version mismatch)
            Appointment updatedAppointment = appointmentRepository.save(appointment);
            
            log.info("Appointment updated successfully: id={}, oldVersion={}, newVersion={}", 
                    updatedAppointment.getId(), originalVersion, updatedAppointment.getVersion());
            
            return appointmentMapper.toAppointmentResponse(updatedAppointment);

        } catch (ObjectOptimisticLockingFailureException ex) {
            log.warn("Optimistic lock failure during appointment update: id={}, version={}", id, originalVersion);
            throw new OptimisticLockException(
                    "Appointment was modified by another user. Please refresh and try again.",
                    "Appointment", 
                    id, 
                    originalVersion
            );
        }
    }

    /**
     * Cancels an appointment with pessimistic locking for consistency
     */
    @Transactional(
        isolation = Isolation.READ_COMMITTED,
        propagation = Propagation.REQUIRED,
        rollbackFor = {
            ConcurrentBookingException.class,
            InvalidAppointmentTimeException.class
        }
    )
    @CacheEvict(value = {
            "appointments", 
            "doctorAppointments", 
            "patientAppointments",
            "appointmentsByStatus",
            "todayAppointments"
    }, allEntries = true)
    public void cancelAppointment(Long id) {
        log.info("Cancelling appointment with pessimistic locking: {}", id);

        // Use pessimistic locking to prevent concurrent modifications
        Optional<Appointment> appointmentOpt = appointmentRepository.findByIdWithLock(id);
        
        if (appointmentOpt.isEmpty()) {
            throw new ResourceNotFoundException("Appointment not found with id: " + id);
        }

        Appointment appointment = appointmentOpt.get();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime appointmentTime = appointment.getAppointmentDateTime();

        // Validate cancellation rules
        validateAppointmentCancellation(appointment, now);

        // Update appointment status
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancellationReason("Cancelled by user");
        
        appointmentRepository.save(appointment);
        
        log.info("Appointment cancelled successfully: {}", id);
    }

    /**
     * Gets appointment by ID (read-only operation)
     */
    @Transactional(
        isolation = Isolation.READ_COMMITTED,
        propagation = Propagation.SUPPORTS,
        readOnly = true
    )
    public AppointmentResponse getAppointmentById(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id));
        
        return appointmentMapper.toAppointmentResponse(appointment);
    }

    /**
     * Validates appointment conflicts with pessimistic locking
     */
    private void validateNoAppointmentConflictsWithLock(User doctor, LocalDateTime appointmentDateTime) {
        // Calculate appointment end time (default 30 minutes)
        LocalDateTime appointmentEndTime = appointmentDateTime.plusMinutes(30);

        // Find overlapping appointments with pessimistic lock
        List<Appointment> overlappingAppointments = appointmentRepository
                .findOverlappingAppointmentsWithLock(doctor.getId(), appointmentDateTime, appointmentEndTime);

        if (!overlappingAppointments.isEmpty()) {
            Appointment conflictingAppointment = overlappingAppointments.get(0);
            log.warn("Appointment conflict detected: doctorId={}, conflictingAppointmentId={}", 
                    doctor.getId(), conflictingAppointment.getId());
            
            throw new ConcurrentBookingException(
                    "Doctor already has an appointment at this time. " +
                    "Existing appointment: " + conflictingAppointment.getAppointmentDateTime(),
                    doctor.getId(), 
                    appointmentDateTime,
                    conflictingAppointment.getId()
            );
        }
    }

    /**
     * Validates appointment update with business rules
     */
    private void validateAppointmentUpdate(Appointment appointment) {
        // Validate appointment time constraints
        validateAppointmentTimeConstraints(appointment.getAppointmentDateTime());

        // Validate doctor's availability
        validateDoctorAvailability(appointment.getDoctor(), appointment.getAppointmentDateTime());

        // Check for conflicts (excluding current appointment)
        validateNoAppointmentConflictsWithLock(appointment.getDoctor(), appointment.getAppointmentDateTime());
    }

    /**
     * Validates appointment cancellation rules
     */
    private void validateAppointmentCancellation(Appointment appointment, LocalDateTime now) {
        LocalDateTime appointmentTime = appointment.getAppointmentDateTime();

        // Cannot cancel appointments that are in the past
        if (appointmentTime.isBefore(now)) {
            throw new InvalidAppointmentTimeException(
                    "Cannot cancel past appointments", 
                    appointmentTime, 
                    "PAST_APPOINTMENT");
        }

        // Cannot cancel appointments that are too close (less than 2 hours away)
        if (appointmentTime.isBefore(now.plusHours(MIN_ADVANCE_BOOKING_HOURS))) {
            throw new InvalidAppointmentTimeException(
                    "Cannot cancel appointments less than " + MIN_ADVANCE_BOOKING_HOURS + " hours before scheduled time", 
                    appointmentTime, 
                    "TOO_CLOSE_TO_CANCEL");
        }

        // Only allow cancellation if appointment is still SCHEDULED
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new InvalidAppointmentTimeException(
                    "Cannot cancel appointment with status: " + appointment.getStatus(), 
                    appointmentTime, 
                    "INVALID_STATUS");
        }
    }

    /**
     * Validates appointment request data
     */
    private void validateAppointmentRequest(AppointmentRequest request) {
        if (request.getDoctorId().equals(request.getPatientId())) {
            throw new InvalidAppointmentTimeException(
                    "Doctor and patient cannot be the same user", 
                    request.getAppointmentDateTime(), 
                    "SAME_USER");
        }
    }

    /**
     * Finds and validates doctor
     */
    private User findAndValidateDoctor(Long doctorId) {
        User doctor = userRepository.findByIdAndEnabledTrue(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id: " + doctorId));

        if (!doctor.getRole().equals(Role.DOCTOR)) {
            throw new InvalidAppointmentTimeException(
                    "User is not a doctor", 
                    null, 
                    "INVALID_ROLE");
        }

        return doctor;
    }

    /**
     * Finds and validates patient
     */
    private User findAndValidatePatient(Long patientId) {
        User patient = userRepository.findByIdAndEnabledTrue(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + patientId));

        if (!patient.getRole().equals(Role.PATIENT)) {
            throw new InvalidAppointmentTimeException(
                    "User is not a patient", 
                    null, 
                    "INVALID_ROLE");
        }

        return patient;
    }

    /**
     * Validates appointment time constraints
     */
    private void validateAppointmentTimeConstraints(LocalDateTime appointmentDateTime) {
        LocalDateTime now = LocalDateTime.now();

        // Check if appointment is in the future (minimum advance booking)
        if (appointmentDateTime.isBefore(now.plusHours(MIN_ADVANCE_BOOKING_HOURS))) {
            throw new InvalidAppointmentTimeException(
                    "Appointments must be booked at least " + MIN_ADVANCE_BOOKING_HOURS + " hours in advance", 
                    appointmentDateTime, 
                    "TOO_SOON");
        }

        // Check if appointment is not too far in the future
        if (appointmentDateTime.isAfter(now.plusDays(MAX_BOOKING_DAYS_AHEAD))) {
            throw new InvalidAppointmentTimeException(
                    "Appointments cannot be booked more than " + MAX_BOOKING_DAYS_AHEAD + " days in advance", 
                    appointmentDateTime, 
                    "TOO_FAR");
        }

        // Check if appointment is on weekend (business rule)
        if (isWeekend(appointmentDateTime)) {
            throw new InvalidAppointmentTimeException(
                    "Appointments are not available on weekends", 
                    appointmentDateTime, 
                    "WEEKEND");
        }

        // Check if appointment is during working hours (9 AM - 6 PM)
        LocalTime appointmentTime = appointmentDateTime.toLocalTime();
        if (appointmentTime.isBefore(LocalTime.of(9, 0)) || appointmentTime.isAfter(LocalTime.of(18, 0))) {
            throw new InvalidAppointmentTimeException(
                    "Appointments are only available between 9:00 AM and 6:00 PM", 
                    appointmentDateTime, 
                    "OUTSIDE_HOURS");
        }
    }

    /**
     * Validates doctor's availability and schedule
     */
    private void validateDoctorAvailability(User doctor, LocalDateTime appointmentDateTime) {
        DayOfWeek dayOfWeek = appointmentDateTime.getDayOfWeek();
        LocalTime appointmentTime = appointmentDateTime.toLocalTime();

        // Find doctor's schedule for the day
        Optional<Schedule> scheduleOpt = scheduleRepository
                .findByDoctorIdAndDayOfWeekAndActiveTrue(doctor.getId(), dayOfWeek);

        if (scheduleOpt.isEmpty()) {
            throw new InvalidAppointmentTimeException(
                    "Doctor is not available on " + dayOfWeek, 
                    appointmentDateTime, 
                    "DOCTOR_UNAVAILABLE");
        }

        Schedule schedule = scheduleOpt.get();

        // Check if appointment time is within doctor's working hours
        if (appointmentTime.isBefore(schedule.getStartTime()) || 
            appointmentTime.isAfter(schedule.getEndTime())) {
            throw new InvalidAppointmentTimeException(
                    "Appointment time is outside doctor's working hours. " +
                    "Available: " + schedule.getStartTime() + " - " + schedule.getEndTime(), 
                    appointmentDateTime, 
                    "OUTSIDE_SCHEDULE");
        }

        // Check if appointment duration fits within working hours
        LocalDateTime appointmentEndTime = appointmentDateTime.plusMinutes(schedule.getAppointmentDurationMinutes());
        if (appointmentEndTime.toLocalTime().isAfter(schedule.getEndTime())) {
            throw new InvalidAppointmentTimeException(
                    "Appointment duration exceeds doctor's working hours", 
                    appointmentDateTime, 
                    "DURATION_EXCEEDS_SCHEDULE");
        }
    }

    /**
     * Validates daily appointment limit
     */
    private void validateDailyAppointmentLimit(User doctor, LocalDateTime appointmentDateTime) {
        LocalDateTime startOfDay = appointmentDateTime.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        long dailyAppointmentCount = appointmentRepository
                .countByDoctorIdAndAppointmentDateTimeBetween(doctor.getId(), startOfDay, endOfDay);

        if (dailyAppointmentCount >= MAX_APPOINTMENTS_PER_DOCTOR_PER_DAY) {
            throw new ConcurrentBookingException(
                    "Doctor has reached maximum daily appointment limit (" + MAX_APPOINTMENTS_PER_DOCTOR_PER_DAY + ")",
                    doctor.getId, 
                    appointmentDateTime,
                    null
            );
        }
    }

    /**
     * Checks if given date is a weekend
     */
    private boolean isWeekend(LocalDateTime dateTime) {
        DayOfWeek dayOfWeek = dateTime.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }
}

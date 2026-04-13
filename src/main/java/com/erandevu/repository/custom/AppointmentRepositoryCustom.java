package com.erandevu.repository.custom;

import com.erandevu.entity.Appointment;
import com.erandevu.enums.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Custom repository interface for complex appointment queries.
 * Implements the Repository pattern for clean separation of concerns.
 */
public interface AppointmentRepositoryCustom {

    /**
     * Finds overlapping appointments for a doctor within a time range.
     * Uses pessimistic locking to prevent concurrent booking conflicts.
     *
     * @param doctorId the doctor ID
     * @param startTime the start of the time range
     * @param endTime the end of the time range
     * @return list of conflicting appointments
     */
    List<Appointment> findOverlappingAppointments(Long doctorId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Checks if there are any overlapping appointments for a doctor.
     * Optimized for conflict detection without loading full entities.
     *
     * @param doctorId the doctor ID
     * @param startTime the start time
     * @param endTime the end time
     * @return true if conflicts exist, false otherwise
     */
    boolean hasOverlappingAppointments(Long doctorId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Finds an appointment by ID with pessimistic locking.
     * Used for critical operations like cancellation.
     *
     * @param id the appointment ID
     * @return optional containing the locked appointment
     */
    Optional<Appointment> findByIdWithLock(Long id);

    /**
     * Counts daily appointments for a doctor on a specific date.
     * Excludes cancelled and no-show appointments.
     *
     * @param doctorId the doctor ID
     * @param date the date to count
     * @return count of appointments
     */
    long countDailyAppointments(Long doctorId, LocalDateTime date);

    /**
     * Finds upcoming appointments for a doctor.
     * Paginated for performance with large datasets.
     *
     * @param doctorId the doctor ID
     * @param fromDate the start date
     * @param statuses the statuses to include
     * @param pageable pagination information
     * @return page of appointments
     */
    Page<Appointment> findUpcomingByDoctor(Long doctorId, LocalDateTime fromDate,
                                           List<AppointmentStatus> statuses, Pageable pageable);

    /**
     * Finds upcoming appointments for a patient.
     * Paginated for performance with large datasets.
     *
     * @param patientId the patient ID
     * @param fromDate the start date
     * @param statuses the statuses to include
     * @param pageable pagination information
     * @return page of appointments
     */
    Page<Appointment> findUpcomingByPatient(Long patientId, LocalDateTime fromDate,
                                              List<AppointmentStatus> statuses, Pageable pageable);
}

package com.erandevu.repository;

import com.erandevu.entity.Appointment;
import com.erandevu.enums.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConcurrentAppointmentRepository extends JpaRepository<Appointment, Long> {

    /**
     * Find overlapping appointments with PESSIMISTIC_WRITE lock
     * This prevents other transactions from modifying overlapping appointments
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Appointment a WHERE a.doctor.id = :doctorId " +
           "AND a.isDeleted = false " +
           "AND a.status != 'CANCELLED' " +
           "AND ((a.appointmentDateTime <= :startTime AND a.endDateTime > :startTime) " +
           "OR (a.appointmentDateTime < :endTime AND a.endDateTime >= :endTime) " +
           "OR (a.appointmentDateTime >= :startTime AND a.endDateTime <= :endTime))")
    List<Appointment> findOverlappingAppointmentsWithLock(
            @Param("doctorId") Long doctorId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * Find appointment by ID with PESSIMISTIC_WRITE lock
     * Used for critical operations like status updates
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Appointment a WHERE a.id = :id AND a.isDeleted = false")
    Optional<Appointment> findByIdWithLock(@Param("id") Long id);

    /**
     * Check if doctor has appointments in time range (without locking for read operations)
     */
    @Query("SELECT COUNT(a) > 0 FROM Appointment a WHERE a.doctor.id = :doctorId " +
           "AND a.isDeleted = false " +
           "AND a.status != 'CANCELLED' " +
           "AND a.appointmentDateTime >= :startTime " +
           "AND a.appointmentDateTime < :endTime")
    boolean hasAppointmentsInTimeRange(
            @Param("doctorId") Long doctorId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * Count appointments for a doctor on a specific day
     */
    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.doctor.id = :doctorId " +
           "AND a.isDeleted = false " +
           "AND a.status != 'CANCELLED' " +
           "AND a.appointmentDateTime >= :startDate " +
           "AND a.appointmentDateTime < :endDate")
    long countByDoctorIdAndAppointmentDateTimeBetween(
            @Param("doctorId") Long doctorId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find appointments for a doctor on a specific date
     */
    @Query("SELECT a FROM Appointment a WHERE a.doctor.id = :doctorId " +
           "AND a.isDeleted = false " +
           "AND DATE(a.appointmentDateTime) = DATE(:date) " +
           "ORDER BY a.appointmentDateTime")
    List<Appointment> findByDoctorIdAndDate(
            @Param("doctorId") Long doctorId,
            @Param("date") LocalDateTime date
    );

    /**
     * Find today's appointments for a doctor
     */
    @Query("SELECT a FROM Appointment a WHERE a.doctor.id = :doctorId " +
           "AND a.isDeleted = false " +
           "AND DATE(a.appointmentDateTime) = CURRENT_DATE " +
           "ORDER BY a.appointmentDateTime")
    List<Appointment> findTodayAppointmentsByDoctorId(@Param("doctorId") Long doctorId);

    /**
     * Find appointments by status with pagination
     */
    Page<Appointment> findByStatusAndIsDeletedFalse(AppointmentStatus status, Pageable pageable);

    /**
     * Find appointments for a doctor within a date range
     */
    @Query("SELECT a FROM Appointment a WHERE a.doctor.id = :doctorId " +
           "AND a.isDeleted = false " +
           "AND a.appointmentDateTime >= :startDate " +
           "AND a.appointmentDateTime <= :endDate " +
           "ORDER BY a.appointmentDateTime")
    Page<Appointment> findByDoctorIdAndAppointmentDateTimeBetween(
            @Param("doctorId") Long doctorId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    /**
     * Find appointments that need cancellation reminder (within 2 hours)
     */
    @Query("SELECT a FROM Appointment a WHERE a.isDeleted = false " +
           "AND a.status = 'SCHEDULED' " +
           "AND a.appointmentDateTime BETWEEN :startTime AND :endTime")
    List<Appointment> findAppointmentsNeedingReminder(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}

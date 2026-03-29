package com.erandevu.repository;

import com.erandevu.entity.Appointment;
import com.erandevu.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    /**
     * Find overlapping appointments for a doctor within a time range
     */
    @Query("SELECT a FROM Appointment a WHERE a.doctor.id = :doctorId " +
           "AND a.isDeleted = false " +
           "AND a.status != 'CANCELLED' " +
           "AND ((a.appointmentDateTime <= :startTime AND a.endDateTime > :startTime) " +
           "OR (a.appointmentDateTime < :endTime AND a.endDateTime >= :endTime) " +
           "OR (a.appointmentDateTime >= :startTime AND a.endDateTime <= :endTime))")
    List<Appointment> findOverlappingAppointments(
            @Param("doctorId") Long doctorId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * Count appointments for a doctor on a specific day
     */
    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.doctor.id = :doctorId " +
           "AND a.isDeleted = false " +
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
     * Find upcoming appointments for a patient
     */
    List<Appointment> findByPatientIdAndAppointmentDateTimeAfterAndIsDeletedFalseOrderByAppointmentDateTime(
            Long patientId, LocalDateTime dateTime
    );

    /**
     * Find appointments by status
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
     * Find today's appointments for a doctor
     */
    @Query("SELECT a FROM Appointment a WHERE a.doctor.id = :doctorId " +
           "AND a.isDeleted = false " +
           "AND DATE(a.appointmentDateTime) = CURRENT_DATE " +
           "ORDER BY a.appointmentDateTime")
    List<Appointment> findTodayAppointmentsByDoctorId(@Param("doctorId") Long doctorId);

    /**
     * Count total appointments for a doctor
     */
    long countByDoctorIdAndIsDeletedFalse(Long doctorId);

    /**
     * Find cancelled appointments for reporting
     */
    Page<Appointment> findByStatusAndIsDeletedFalse(AppointmentStatus status, Pageable pageable);
}

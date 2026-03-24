package com.erandevu.repository;

import com.erandevu.entity.Appointment;
import com.erandevu.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    
    boolean existsByDoctorIdAndAppointmentDateTime(Long doctorId, LocalDateTime appointmentDateTime);
    
    @Query("SELECT a FROM Appointment a WHERE a.doctor.id = :doctorId AND " +
           "a.appointmentDateTime BETWEEN :startTime AND :endTime AND " +
           "a.status NOT IN ('CANCELLED', 'NO_SHOW')")
    List<Appointment> findConflictingAppointments(@Param("doctorId") Long doctorId,
                                                   @Param("startTime") LocalDateTime startTime,
                                                   @Param("endTime") LocalDateTime endTime);
    
    List<Appointment> findByDoctorIdAndStatusNotIn(Long doctorId, List<AppointmentStatus> statuses);
    
    List<Appointment> findByPatientIdAndStatusNotIn(Long patientId, List<AppointmentStatus> statuses);
    
    List<Appointment> findByDoctorIdAndAppointmentDateTimeBetween(Long doctorId, 
                                                                 LocalDateTime start, 
                                                                 LocalDateTime end);
    
    Optional<Appointment> findByIdAndDoctorId(Long id, Long doctorId);
    
    Optional<Appointment> findByIdAndPatientId(Long id, Long patientId);
}

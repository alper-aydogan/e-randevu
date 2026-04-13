package com.erandevu.repository;

import com.erandevu.entity.Appointment;
import com.erandevu.enums.AppointmentStatus;
import com.erandevu.repository.custom.AppointmentRepositoryCustom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Temel randevu repository arayüzü.
 * JpaRepository ve AppointmentRepositoryCustom'i extend eder.
 * Soft delete @Where annotasyonu ile otomatik yönetilir.
 */
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long>, AppointmentRepositoryCustom {

    /**
     * Randevu varlığını kontrol eder.
     * Soft delete otomatik filtrelenir (@Where clause).
     */
    boolean existsByDoctorIdAndAppointmentDateTime(Long doctorId, LocalDateTime appointmentDateTime);

    /**
     * Doktora ait randevuları getirir.
     * İptal edilmiş ve gelmeyen randevular hariç.
     * Soft delete otomatik filtrelenir.
     */
    List<Appointment> findByDoctorIdAndStatusNotIn(Long doctorId, List<AppointmentStatus> statuses);

    /**
     * Hastaya ait randevuları getirir.
     * İptal edilmiş ve gelmeyen randevular hariç.
     * Soft delete otomatik filtrelenir.
     */
    List<Appointment> findByPatientIdAndStatusNotIn(Long patientId, List<AppointmentStatus> statuses);

    /**
     * Doktor ve hasta ID'sine göre randevu bulur.
     * Soft delete otomatik filtrelenir.
     */
    Optional<Appointment> findByIdAndDoctorId(Long id, Long doctorId);

    /**
     * Hasta ve randevu ID'sine göre randevu bulur.
     * Soft delete otomatik filtrelenir.
     */
    Optional<Appointment> findByIdAndPatientId(Long id, Long patientId);

    /**
     * Sayfalama ile doktor randevularını getirir.
     * Soft delete otomatik filtrelenir.
     */
    Page<Appointment> findByDoctorIdAndStatusNotIn(Long doctorId, List<AppointmentStatus> statuses, Pageable pageable);

    /**
     * Sayfalama ile hasta randevularını getirir.
     * Soft delete otomatik filtrelenir.
     */
    Page<Appointment> findByPatientIdAndStatusNotIn(Long patientId, List<AppointmentStatus> statuses, Pageable pageable);

    /**
     * Statüye göre randevuları sayfalama ile getirir.
     * Soft delete otomatik filtrelenir.
     */
    @Query("SELECT a FROM Appointment a WHERE a.status = :status")
    Page<Appointment> findByStatus(@Param("status") AppointmentStatus status, Pageable pageable);

    /**
     * Doktorun belirli tarih aralığındaki randevularını getirir.
     * Soft delete otomatik filtrelenir.
     */
    List<Appointment> findByDoctorIdAndAppointmentDateTimeBetween(Long doctorId,
                                                                   LocalDateTime start,
                                                                   LocalDateTime end);

    /**
     * Sayfalama ile doktorun tarih aralığındaki randevularını getirir.
     * Soft delete otomatik filtrelenir.
     */
    Page<Appointment> findByDoctorIdAndAppointmentDateTimeBetween(Long doctorId,
                                                                   LocalDateTime start,
                                                                   LocalDateTime end,
                                                                   Pageable pageable);
}

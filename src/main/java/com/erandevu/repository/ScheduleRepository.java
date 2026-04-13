package com.erandevu.repository;

import com.erandevu.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

/**
 * Çalışma takvimi repository arayüzü.
 * Minimal ve temiz yapı - sadece gerekli metodlar.
 * Soft delete @Where annotasyonu ile otomatik yönetilir.
 */
@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    /**
     * Doktora ait aktif çalışma takvimlerini getirir.
     * Soft delete otomatik filtrelenir.
     */
    List<Schedule> findByDoctorIdAndActiveTrue(Long doctorId);

    /**
     * Doktorun belirli gün için aktif takvimini bulur.
     * Soft delete otomatik filtrelenir.
     */
    Optional<Schedule> findByDoctorIdAndDayOfWeekAndActiveTrue(Long doctorId, DayOfWeek dayOfWeek);
}

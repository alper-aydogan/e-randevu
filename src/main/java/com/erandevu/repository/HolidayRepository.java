package com.erandevu.repository;

import com.erandevu.entity.Holiday;
import com.erandevu.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    /**
     * Find holidays for a doctor within a date range
     */
    @Query("SELECT h FROM Holiday h WHERE h.doctor.id = :doctorId " +
           "AND h.isDeleted = false " +
           "AND h.holidayDate >= :startDate " +
           "AND h.holidayDate <= :endDate " +
           "ORDER BY h.holidayDate")
    List<Holiday> findByDoctorIdAndHolidayDateBetween(
            @Param("doctorId") Long doctorId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Find holidays for a doctor on a specific date
     */
    List<Holiday> findByDoctorIdAndHolidayDateAndIsDeletedFalse(Long doctorId, LocalDate date);

    /**
     * Check if doctor has holiday on specific date
     */
    @Query("SELECT COUNT(h) > 0 FROM Holiday h WHERE h.doctor.id = :doctorId " +
           "AND h.holidayDate = :date " +
           "AND h.isDeleted = false")
    boolean existsByDoctorIdAndHolidayDate(
            @Param("doctorId") Long doctorId,
            @Param("date") LocalDate date
    );

    /**
     * Find recurring holidays
     */
    List<Holiday> findByRecurringTrueAndIsDeletedFalse();
}

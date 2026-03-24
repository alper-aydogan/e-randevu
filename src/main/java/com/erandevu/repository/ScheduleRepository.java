package com.erandevu.repository;

import com.erandevu.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    
    List<Schedule> findByDoctorIdAndActiveTrue(Long doctorId);
    
    Optional<Schedule> findByDoctorIdAndDayOfWeekAndActiveTrue(Long doctorId, DayOfWeek dayOfWeek);
    
    @Query("SELECT s FROM Schedule s WHERE s.doctor.id = :doctorId AND " +
           "s.dayOfWeek = :dayOfWeek AND s.active = true")
    Optional<Schedule> findActiveSchedule(@Param("doctorId") Long doctorId, 
                                       @Param("dayOfWeek") DayOfWeek dayOfWeek);
}

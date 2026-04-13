package com.erandevu.service.validation;

import com.erandevu.entity.Schedule;
import com.erandevu.exception.BusinessRuleException;
import com.erandevu.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Single Responsibility: Validates doctor availability for a given time slot.
 * Checks: working schedule, working hours, appointment duration fits.
 */
@Component
@RequiredArgsConstructor
public class DoctorAvailabilityValidator {

    private final ScheduleRepository scheduleRepository;
    public static final int DEFAULT_APPOINTMENT_DURATION_MINUTES = 30;

    public void validate(Long doctorId, LocalDateTime appointmentDateTime) {
        DayOfWeek dayOfWeek = appointmentDateTime.getDayOfWeek();

        Schedule schedule = scheduleRepository
            .findByDoctorIdAndDayOfWeekAndActiveTrue(doctorId, dayOfWeek)
            .orElseThrow(() -> new BusinessRuleException(
                "Doctor is not available on " + dayOfWeek + ". No schedule found."
            ));

        validateWithinWorkingHours(appointmentDateTime, schedule);
        validateAppointmentFitsInSchedule(appointmentDateTime, schedule);
    }

    private void validateWithinWorkingHours(LocalDateTime appointmentDateTime, Schedule schedule) {
        LocalTime appointmentTime = appointmentDateTime.toLocalTime();

        if (appointmentTime.isBefore(schedule.getStartTime()) ||
            appointmentTime.isAfter(schedule.getEndTime())) {
            throw new BusinessRuleException(
                "Appointment time is outside doctor's working hours. " +
                "Available: " + schedule.getStartTime() + " - " + schedule.getEndTime()
            );
        }
    }

    private void validateAppointmentFitsInSchedule(LocalDateTime appointmentDateTime, Schedule schedule) {
        int duration = schedule.getAppointmentDurationMinutes() != null
            ? schedule.getAppointmentDurationMinutes()
            : DEFAULT_APPOINTMENT_DURATION_MINUTES;

        LocalDateTime appointmentEnd = appointmentDateTime.plusMinutes(duration);

        if (appointmentEnd.toLocalTime().isAfter(schedule.getEndTime())) {
            throw new BusinessRuleException("Appointment duration exceeds doctor's working hours");
        }
    }
}

package com.erandevu.service;

import com.erandevu.entity.Appointment;
import com.erandevu.entity.Schedule;
import com.erandevu.entity.User;
import com.erandevu.enums.AppointmentStatus;
import com.erandevu.exception.BusinessRuleException;
import com.erandevu.repository.AppointmentRepository;
import com.erandevu.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentValidationService {

    private final ScheduleRepository scheduleRepository;
    private final AppointmentRepository appointmentRepository;

    // Configuration constants
    private static final int MAX_APPOINTMENTS_PER_DOCTOR_PER_DAY = 8;
    private static final int MIN_ADVANCE_BOOKING_HOURS = 2;
    private static final int MAX_BOOKING_DAYS_AHEAD = 30;

    /**
     * Validates appointment creation against business rules
     */
    public void validateAppointmentCreation(Appointment appointment) {
        log.debug("Validating appointment: {}", appointment);

        validateAppointmentDateTime(appointment);
        validateDoctorSchedule(appointment);
        validateNoOverlappingAppointments(appointment);
        validateDailyAppointmentLimit(appointment);
        validateDoctorAvailability(appointment);
    }

    /**
     * Validates appointment date and time
     */
    private void validateAppointmentDateTime(Appointment appointment) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime appointmentTime = appointment.getAppointmentDateTime();

        // No past date appointments
        if (appointmentTime.isBefore(now.plusHours(MIN_ADVANCE_BOOKING_HOURS))) {
            throw new BusinessRuleException(
                "Appointments must be booked at least " + MIN_ADVANCE_BOOKING_HOURS + " hours in advance"
            );
        }

        // No appointments too far in the future
        if (appointmentTime.isAfter(now.plusDays(MAX_BOOKING_DAYS_AHEAD))) {
            throw new BusinessRuleException(
                "Appointments cannot be booked more than " + MAX_BOOKING_DAYS_AHEAD + " days in advance"
            );
        }

        // No appointments on weekends (if business rule applies)
        if (isWeekend(appointmentTime)) {
            throw new BusinessRuleException("Appointments are not available on weekends");
        }
    }

    /**
     * Validates against doctor's working schedule
     */
    private void validateDoctorSchedule(Appointment appointment) {
        User doctor = appointment.getDoctor();
        LocalDateTime appointmentTime = appointment.getAppointmentDateTime();
        DayOfWeek dayOfWeek = appointmentTime.getDayOfWeek();
        LocalTime appointmentStartTime = appointmentTime.toLocalTime();

        Optional<Schedule> scheduleOpt = scheduleRepository
                .findByDoctorIdAndDayOfWeekAndActiveTrue(doctor.getId(), dayOfWeek);

        if (scheduleOpt.isEmpty()) {
            throw new BusinessRuleException(
                    "Doctor is not available on " + dayOfWeek
            );
        }

        Schedule schedule = scheduleOpt.get();

        // Check if appointment is within working hours
        if (appointmentStartTime.isBefore(schedule.getStartTime()) ||
            appointmentStartTime.isAfter(schedule.getEndTime())) {
            throw new BusinessRuleException(
                    "Appointment time is outside doctor's working hours. " +
                    "Available: " + schedule.getStartTime() + " - " + schedule.getEndTime()
            );
        }

        // Check appointment duration fits within working hours
        LocalDateTime appointmentEndTime = appointmentTime.plusMinutes(schedule.getAppointmentDurationMinutes());
        LocalTime scheduleEndTime = schedule.getEndTime();

        if (appointmentEndTime.toLocalTime().isAfter(scheduleEndTime)) {
            throw new BusinessRuleException(
                    "Appointment duration exceeds doctor's working hours"
            );
        }
    }

    /**
     * Validates no overlapping appointments
     */
    private void validateNoOverlappingAppointments(Appointment appointment) {
        User doctor = appointment.getDoctor();
        LocalDateTime appointmentTime = appointment.getAppointmentDateTime();
        LocalDateTime appointmentEndTime = appointment.getEndDateTime();

        List<Appointment> overlappingAppointments = appointmentRepository
                .findOverlappingAppointments(doctor.getId(), appointmentTime, appointmentEndTime);

        if (!overlappingAppointments.isEmpty()) {
            throw new BusinessRuleException(
                    "Doctor already has an appointment at this time"
            );
        }
    }

    /**
     * Validates daily appointment limit
     */
    private void validateDailyAppointmentLimit(Appointment appointment) {
        User doctor = appointment.getDoctor();
        LocalDateTime appointmentDate = appointment.getAppointmentDateTime().toLocalDate().atStartOfDay();

        long dailyAppointmentCount = appointmentRepository
                .countByDoctorIdAndAppointmentDateTimeBetween(
                        doctor.getId(),
                        appointmentDate,
                        appointmentDate.plusDays(1)
                );

        if (dailyAppointmentCount >= MAX_APPOINTMENTS_PER_DOCTOR_PER_DAY) {
            throw new BusinessRuleException(
                    "Doctor has reached maximum daily appointment limit (" + MAX_APPOINTMENTS_PER_DOCTOR_PER_DAY + ")"
            );
        }
    }

    /**
     * Validates doctor is not on holiday/unavailable
     */
    private void validateDoctorAvailability(Appointment appointment) {
        User doctor = appointment.getDoctor();
        LocalDateTime appointmentDate = appointment.getAppointmentDateTime().toLocalDate().atStartOfDay();

        // Check if doctor has any schedule for this day (indicates availability)
        List<Schedule> daySchedules = scheduleRepository
                .findByDoctorIdAndDayOfWeekAndActiveTrue(doctor.getId(), appointmentDate.getDayOfWeek());

        if (daySchedules.isEmpty()) {
            throw new BusinessRuleException(
                    "Doctor is not available on this date"
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

    /**
     * Validates appointment modification
     */
    public void validateAppointmentModification(Appointment existingAppointment, Appointment updatedAppointment) {
        // Only allow modification if appointment is still SCHEDULED
        if (existingAppointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new BusinessRuleException(
                    "Cannot modify appointment with status: " + existingAppointment.getStatus()
            );
        }

        // Re-validate all business rules for the updated appointment
        validateAppointmentCreation(updatedAppointment);
    }

    /**
     * Validates appointment cancellation
     */
    public void validateAppointmentCancellation(Appointment appointment) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime appointmentTime = appointment.getAppointmentDateTime();

        // Cannot cancel appointments that are in the past
        if (appointmentTime.isBefore(now)) {
            throw new BusinessRuleException("Cannot cancel past appointments");
        }

        // Cannot cancel appointments that are too close (e.g., less than 2 hours away)
        if (appointmentTime.isBefore(now.plusHours(MIN_ADVANCE_BOOKING_HOURS))) {
            throw new BusinessRuleException(
                    "Cannot cancel appointments less than " + MIN_ADVANCE_BOOKING_HOURS + " hours before scheduled time"
            );
        }
    }
}

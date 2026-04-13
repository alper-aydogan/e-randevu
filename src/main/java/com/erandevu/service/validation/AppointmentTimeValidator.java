package com.erandevu.service.validation;

import com.erandevu.exception.InvalidAppointmentTimeException;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Single Responsibility: Validates appointment time constraints.
 * Checks: business hours, weekends, advance booking, max future booking.
 */
@Component
public class AppointmentTimeValidator {

    public static final int MIN_ADVANCE_BOOKING_HOURS = 2;
    public static final int MAX_BOOKING_DAYS_AHEAD = 30;
    public static final LocalTime BUSINESS_START = LocalTime.of(9, 0);
    public static final LocalTime BUSINESS_END = LocalTime.of(18, 0);

    public void validate(LocalDateTime appointmentDateTime) {
        validateNotInPast(appointmentDateTime);
        validateAdvanceBooking(appointmentDateTime);
        validateMaxFutureBooking(appointmentDateTime);
        validateWeekday(appointmentDateTime);
        validateBusinessHours(appointmentDateTime);
    }

    private void validateNotInPast(LocalDateTime appointmentDateTime) {
        if (appointmentDateTime.isBefore(LocalDateTime.now())) {
            throw new InvalidAppointmentTimeException("Appointment time must be in the future");
        }
    }

    private void validateAdvanceBooking(LocalDateTime appointmentDateTime) {
        if (appointmentDateTime.isBefore(LocalDateTime.now().plusHours(MIN_ADVANCE_BOOKING_HOURS))) {
            throw new InvalidAppointmentTimeException(
                "Appointments must be booked at least " + MIN_ADVANCE_BOOKING_HOURS + " hours in advance"
            );
        }
    }

    private void validateMaxFutureBooking(LocalDateTime appointmentDateTime) {
        if (appointmentDateTime.isAfter(LocalDateTime.now().plusDays(MAX_BOOKING_DAYS_AHEAD))) {
            throw new InvalidAppointmentTimeException(
                "Appointments cannot be booked more than " + MAX_BOOKING_DAYS_AHEAD + " days in advance"
            );
        }
    }

    private void validateWeekday(LocalDateTime appointmentDateTime) {
        DayOfWeek dayOfWeek = appointmentDateTime.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            throw new InvalidAppointmentTimeException("Appointments are not available on weekends");
        }
    }

    private void validateBusinessHours(LocalDateTime appointmentDateTime) {
        LocalTime time = appointmentDateTime.toLocalTime();
        if (time.isBefore(BUSINESS_START) || time.isAfter(BUSINESS_END)) {
            throw new InvalidAppointmentTimeException(
                "Appointments are only available between " + BUSINESS_START + " and " + BUSINESS_END
            );
        }
    }
}

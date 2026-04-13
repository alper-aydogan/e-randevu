package com.erandevu.exception;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Exception thrown when appointment time is invalid.
 * Extends BaseException for standardized error handling.
 */
@Getter
public class InvalidAppointmentTimeException extends BaseException {

    private final LocalDateTime appointmentDateTime;
    private final String violationType;

    public InvalidAppointmentTimeException(String message) {
        super(ErrorCode.APPOINTMENT_TIME_INVALID, message);
        this.appointmentDateTime = null;
        this.violationType = null;
    }

    public InvalidAppointmentTimeException(String message, LocalDateTime appointmentDateTime, String violationType) {
        super(ErrorCode.APPOINTMENT_TIME_INVALID, message, buildDetails(appointmentDateTime, violationType));
        this.appointmentDateTime = appointmentDateTime;
        this.violationType = violationType;
    }

    public InvalidAppointmentTimeException(String message, Throwable cause, LocalDateTime appointmentDateTime, String violationType) {
        super(ErrorCode.APPOINTMENT_TIME_INVALID, message, buildDetails(appointmentDateTime, violationType), cause);
        this.appointmentDateTime = appointmentDateTime;
        this.violationType = violationType;
    }

    private static Map<String, Object> buildDetails(LocalDateTime appointmentDateTime, String violationType) {
        Map<String, Object> details = new HashMap<>();
        if (appointmentDateTime != null) {
            details.put("appointmentDateTime", appointmentDateTime.toString());
        }
        if (violationType != null) {
            details.put("violationType", violationType);
        }
        return details;
    }
}

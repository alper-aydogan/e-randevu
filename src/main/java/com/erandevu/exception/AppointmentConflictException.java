package com.erandevu.exception;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Exception thrown when there's a conflict in appointment booking.
 * Extends BaseException for standardized error handling.
 */
@Getter
public class AppointmentConflictException extends BaseException {

    private final Long doctorId;
    private final LocalDateTime appointmentDateTime;

    public AppointmentConflictException(String message) {
        super(ErrorCode.APPOINTMENT_CONFLICT, message);
        this.doctorId = null;
        this.appointmentDateTime = null;
    }

    public AppointmentConflictException(String message, Long doctorId, LocalDateTime appointmentDateTime) {
        super(ErrorCode.APPOINTMENT_CONFLICT, message, buildDetails(doctorId, appointmentDateTime));
        this.doctorId = doctorId;
        this.appointmentDateTime = appointmentDateTime;
    }

    public AppointmentConflictException(String message, Throwable cause, Long doctorId, LocalDateTime appointmentDateTime) {
        super(ErrorCode.APPOINTMENT_CONFLICT, message, buildDetails(doctorId, appointmentDateTime), cause);
        this.doctorId = doctorId;
        this.appointmentDateTime = appointmentDateTime;
    }

    private static Map<String, Object> buildDetails(Long doctorId, LocalDateTime appointmentDateTime) {
        Map<String, Object> details = new HashMap<>();
        if (doctorId != null) {
            details.put("doctorId", doctorId);
        }
        if (appointmentDateTime != null) {
            details.put("appointmentDateTime", appointmentDateTime.toString());
        }
        return details;
    }
}

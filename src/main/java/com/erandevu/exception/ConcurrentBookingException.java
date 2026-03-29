package com.erandevu.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.LocalDateTime;

@Getter
@ResponseStatus(HttpStatus.CONFLICT)
public class ConcurrentBookingException extends RuntimeException {
    private final Long doctorId;
    private final LocalDateTime appointmentDateTime;
    private final Long conflictingAppointmentId;
    
    public ConcurrentBookingException(String message, Long doctorId, LocalDateTime appointmentDateTime, Long conflictingAppointmentId) {
        super(message);
        this.doctorId = doctorId;
        this.appointmentDateTime = appointmentDateTime;
        this.conflictingAppointmentId = conflictingAppointmentId;
    }
    
    public ConcurrentBookingException(String message, Throwable cause, Long doctorId, LocalDateTime appointmentDateTime, Long conflictingAppointmentId) {
        super(message, cause);
        this.doctorId = doctorId;
        this.appointmentDateTime = appointmentDateTime;
        this.conflictingAppointmentId = conflictingAppointmentId;
    }
}

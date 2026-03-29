package com.erandevu.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.LocalDateTime;

@ResponseStatus(HttpStatus.CONFLICT)
@Getter
public class AppointmentConflictException extends RuntimeException {
    private final Long doctorId;
    private final LocalDateTime appointmentDateTime;
    
    public AppointmentConflictException(String message) {
        super(message);
        this.doctorId = null;
        this.appointmentDateTime = null;
    }
    
    public AppointmentConflictException(String message, Long doctorId, LocalDateTime appointmentDateTime) {
        super(message);
        this.doctorId = doctorId;
        this.appointmentDateTime = appointmentDateTime;
    }
    
    public AppointmentConflictException(String message, Throwable cause, Long doctorId, LocalDateTime appointmentDateTime) {
        super(message, cause);
        this.doctorId = doctorId;
        this.appointmentDateTime = appointmentDateTime;
    }
}

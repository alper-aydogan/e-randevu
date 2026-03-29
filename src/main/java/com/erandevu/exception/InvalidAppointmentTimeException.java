package com.erandevu.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.LocalDateTime;

@ResponseStatus(HttpStatus.BAD_REQUEST)
@Getter
public class InvalidAppointmentTimeException extends RuntimeException {
    private final LocalDateTime appointmentDateTime;
    private final String violationType;
    
    public InvalidAppointmentTimeException(String message) {
        super(message);
        this.appointmentDateTime = null;
        this.violationType = null;
    }
    
    public InvalidAppointmentTimeException(String message, LocalDateTime appointmentDateTime, String violationType) {
        super(message);
        this.appointmentDateTime = appointmentDateTime;
        this.violationType = violationType;
    }
    
    public InvalidAppointmentTimeException(String message, Throwable cause, LocalDateTime appointmentDateTime, String violationType) {
        super(message, cause);
        this.appointmentDateTime = appointmentDateTime;
        this.violationType = violationType;
    }
}

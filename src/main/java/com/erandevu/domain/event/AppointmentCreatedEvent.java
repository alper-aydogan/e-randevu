package com.erandevu.domain.event;

import com.erandevu.entity.Appointment;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Domain Event: Published when a new appointment is created.
 */
@Getter
public class AppointmentCreatedEvent {

    private final Long appointmentId;
    private final Long doctorId;
    private final Long patientId;
    private final LocalDateTime appointmentDateTime;
    private final LocalDateTime occurredAt;

    public AppointmentCreatedEvent(Appointment appointment) {
        this.appointmentId = appointment.getId();
        this.doctorId = appointment.getDoctor().getId();
        this.patientId = appointment.getPatient().getId();
        this.appointmentDateTime = appointment.getAppointmentDateTime();
        this.occurredAt = LocalDateTime.now();
    }
}

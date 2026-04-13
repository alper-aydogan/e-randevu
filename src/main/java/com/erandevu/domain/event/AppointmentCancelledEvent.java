package com.erandevu.domain.event;

import com.erandevu.entity.Appointment;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Domain Event: Published when an appointment is cancelled.
 */
@Getter
public class AppointmentCancelledEvent {

    private final Long appointmentId;
    private final Long doctorId;
    private final Long patientId;
    private final LocalDateTime appointmentDateTime;
    private final String cancellationReason;
    private final LocalDateTime occurredAt;

    public AppointmentCancelledEvent(Appointment appointment) {
        this.appointmentId = appointment.getId();
        this.doctorId = appointment.getDoctor().getId();
        this.patientId = appointment.getPatient().getId();
        this.appointmentDateTime = appointment.getAppointmentDateTime();
        this.cancellationReason = appointment.getCancellationReason();
        this.occurredAt = LocalDateTime.now();
    }
}

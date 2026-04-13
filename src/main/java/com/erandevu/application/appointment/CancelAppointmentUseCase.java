package com.erandevu.application.appointment;

import com.erandevu.domain.event.AppointmentCancelledEvent;
import com.erandevu.dto.response.AppointmentResponse;
import com.erandevu.entity.Appointment;
import com.erandevu.exception.ResourceNotFoundException;
import com.erandevu.mapper.AppointmentMapper;
import com.erandevu.repository.AppointmentRepository;
import com.erandevu.service.validation.AppointmentValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application Use Case: Cancel Appointment.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CancelAppointmentUseCase {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentValidator validator;
    private final AppointmentMapper appointmentMapper;
    private final ApplicationEventPublisher eventPublisher;

    public AppointmentResponse execute(Long appointmentId, String reason) {
        log.info("UC: Cancelling appointment - id={}", appointmentId);

        // 1. Fetch with lock
        Appointment appointment = appointmentRepository.findByIdWithLock(appointmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        // 2. Validate cancellation rules
        validator.validateCancellation(appointment);

        // 3. Domain logic: cancel
        appointment.cancel(reason);

        // 4. Persist
        Appointment saved = appointmentRepository.save(appointment);
        log.info("UC: Appointment cancelled - id={}", saved.getId());

        // 5. Publish domain event
        eventPublisher.publishEvent(new AppointmentCancelledEvent(saved));

        return appointmentMapper.toAppointmentResponse(saved);
    }
}

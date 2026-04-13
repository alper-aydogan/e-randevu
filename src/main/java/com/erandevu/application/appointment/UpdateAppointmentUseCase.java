package com.erandevu.application.appointment;

import com.erandevu.dto.request.AppointmentRequest;
import com.erandevu.dto.response.AppointmentResponse;
import com.erandevu.entity.Appointment;
import com.erandevu.exception.ResourceNotFoundException;
import com.erandevu.mapper.AppointmentMapper;
import com.erandevu.repository.AppointmentRepository;
import com.erandevu.service.validation.AppointmentValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application Use Case: Update Appointment.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UpdateAppointmentUseCase {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentValidator validator;
    private final AppointmentMapper appointmentMapper;

    public AppointmentResponse execute(Long appointmentId, AppointmentRequest request) {
        log.info("UC: Updating appointment - id={}", appointmentId);

        // 1. Fetch with lock
        Appointment appointment = appointmentRepository.findByIdWithLock(appointmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        // 2. Validate
        validator.validateUpdate(appointment, request);

        // 3. Domain logic: reschedule
        appointment.reschedule(request.getAppointmentDateTime());
        appointment.setNotes(request.getNotes());

        // 4. Persist
        Appointment saved = appointmentRepository.save(appointment);
        log.info("UC: Appointment updated - id={}", saved.getId());

        return appointmentMapper.toAppointmentResponse(saved);
    }
}

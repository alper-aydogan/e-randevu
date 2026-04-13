package com.erandevu.application.appointment;

import com.erandevu.domain.event.AppointmentCreatedEvent;
import com.erandevu.dto.request.AppointmentRequest;
import com.erandevu.dto.response.AppointmentResponse;
import com.erandevu.entity.Appointment;
import com.erandevu.entity.User;
import com.erandevu.enums.AppointmentStatus;
import com.erandevu.exception.AppointmentConflictException;
import com.erandevu.exception.ResourceNotFoundException;
import com.erandevu.mapper.AppointmentMapper;
import com.erandevu.repository.AppointmentRepository;
import com.erandevu.repository.UserRepository;
import com.erandevu.service.validation.AppointmentValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application Use Case: Create Appointment.
 * Orchestrates the creation of a new appointment with validation and event publishing.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CreateAppointmentUseCase {

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final AppointmentValidator validator;
    private final AppointmentMapper appointmentMapper;
    private final ApplicationEventPublisher eventPublisher;

    public AppointmentResponse execute(AppointmentRequest request) {
        log.info("UC: Creating appointment - doctor={}, patient={}",
            request.getDoctorId(), request.getPatientId());

        try {
            // 1. Validate
            validator.validateCreation(request, request.getDoctorId(), request.getPatientId());

            // 2. Fetch entities
            User doctor = userRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));
            User patient = userRepository.findById(request.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

            // 3. Create domain entity (domain logic handles its own state)
            Appointment appointment = Appointment.builder()
                .doctor(doctor)
                .patient(patient)
                .appointmentDateTime(request.getAppointmentDateTime())
                .notes(request.getNotes())
                .status(AppointmentStatus.SCHEDULED)
                .build();

            // 4. Persist
            Appointment saved = appointmentRepository.save(appointment);
            log.info("UC: Appointment created - id={}", saved.getId());

            // 5. Publish domain event
            eventPublisher.publishEvent(new AppointmentCreatedEvent(saved));

            // 6. Return response
            return appointmentMapper.toAppointmentResponse(saved);

        } catch (DataIntegrityViolationException | ObjectOptimisticLockingFailureException e) {
            log.warn("UC: Concurrent booking conflict - doctor={}, time={}",
                request.getDoctorId(), request.getAppointmentDateTime());
            throw new AppointmentConflictException(
                "Slot already booked", request.getDoctorId(), request.getAppointmentDateTime());
        }
    }
}

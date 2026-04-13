package com.erandevu.service;

import com.erandevu.dto.request.AppointmentRequest;
import com.erandevu.dto.response.AppointmentResponse;
import com.erandevu.dto.response.PageResponse;
import com.erandevu.entity.Appointment;
import com.erandevu.entity.User;
import com.erandevu.enums.AppointmentStatus;
import com.erandevu.exception.AppointmentConflictException;
import com.erandevu.exception.ResourceNotFoundException;
import com.erandevu.mapper.AppointmentMapper;
import com.erandevu.repository.AppointmentRepository;
import com.erandevu.repository.UserRepository;
import com.erandevu.service.validation.AppointmentValidator;
import com.erandevu.util.PageResponseUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Appointment Service - Orchestration layer only.
 * Uses rich domain model (Appointment entity has behavior methods).
 * Delegates validation to AppointmentValidator.
 * Handles caching, transactions, and concurrency.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AppointmentService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final AppointmentValidator validator;
    private final AppointmentMapper appointmentMapper;

    // ==================== WRITE OPERATIONS ====================

    @CacheEvict(value = "appointments", key = "'doctor_' + #request.doctorId")
    public AppointmentResponse createAppointment(AppointmentRequest request, Long patientId) {
        log.info("Creating appointment: doctor={}, patient={}, time={}",
            request.getDoctorId(), patientId, request.getAppointmentDateTime());

        try {
            validator.validateCreation(request, request.getDoctorId(), patientId);

            User doctor = findDoctor(request.getDoctorId());
            User patient = findPatient(patientId);

            Appointment appointment = Appointment.builder()
                .doctor(doctor)
                .patient(patient)
                .appointmentDateTime(request.getAppointmentDateTime())
                .notes(request.getNotes())
                .status(AppointmentStatus.SCHEDULED)
                .build();

            Appointment saved = appointmentRepository.save(appointment);
            log.info("Appointment created: id={}", saved.getId());

            return appointmentMapper.toAppointmentResponse(saved);

        } catch (DataIntegrityViolationException | ObjectOptimisticLockingFailureException e) {
            log.warn("Concurrent booking: doctor={}, time={}", request.getDoctorId(), request.getAppointmentDateTime());
            throw new AppointmentConflictException("Slot already booked", request.getDoctorId(), request.getAppointmentDateTime());
        }
    }

    @CacheEvict(value = "appointments", key = "'apt_' + #id")
    public AppointmentResponse updateAppointment(Long id, AppointmentRequest request) {
        log.info("Updating appointment: id={}", id);

        Appointment existing = appointmentRepository.findByIdWithLock(id)
            .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + id));

        validator.validateUpdate(existing, request);

        existing.reschedule(request.getAppointmentDateTime());
        existing.setNotes(request.getNotes());

        Appointment saved = appointmentRepository.save(existing);
        log.info("Appointment updated: id={}", saved.getId());

        return appointmentMapper.toAppointmentResponse(saved);
    }

    @CacheEvict(value = "appointments", key = "'apt_' + #id")
    public AppointmentResponse cancelAppointment(Long id, String reason) {
        log.info("Cancelling appointment: id={}", id);

        Appointment appointment = appointmentRepository.findByIdWithLock(id)
            .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + id));

        validator.validateCancellation(appointment);
        appointment.cancel(reason);

        Appointment saved = appointmentRepository.save(appointment);
        log.info("Appointment cancelled: id={}", saved.getId());

        return appointmentMapper.toAppointmentResponse(saved);
    }

    @CacheEvict(value = "appointments", key = "'apt_' + #id")
    public AppointmentResponse completeAppointment(Long id) {
        log.info("Completing appointment: id={}", id);

        Appointment appointment = appointmentRepository.findByIdWithLock(id)
            .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + id));

        appointment.markCompleted();
        Appointment saved = appointmentRepository.save(appointment);

        log.info("Appointment completed: id={}", saved.getId());
        return appointmentMapper.toAppointmentResponse(saved);
    }

    @CacheEvict(value = "appointments", key = "'apt_' + #id")
    public void deleteAppointment(Long id) {
        log.info("Deleting appointment: id={}", id);
        Appointment appointment = appointmentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + id));
        appointmentRepository.delete(appointment);
    }

    // ==================== READ OPERATIONS ====================

    @Cacheable(value = "appointments", key = "'apt_' + #id")
    @Transactional(readOnly = true)
    public AppointmentResponse getAppointmentById(Long id) {
        return appointmentRepository.findById(id)
            .map(appointmentMapper::toAppointmentResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + id));
    }

    @Cacheable(value = "appointments", key = "'doctor_' + #doctorId + '_page_' + #page + '_' + #size")
    @Transactional(readOnly = true)
    public PageResponse<AppointmentResponse> getDoctorAppointments(Long doctorId, int page, int size, String sortBy, String sortDir) {
        Sort sort = createSort(sortBy, sortDir);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Appointment> pageResult = appointmentRepository.findByDoctorIdAndStatusNotIn(
            doctorId,
            List.of(AppointmentStatus.CANCELLED, AppointmentStatus.NO_SHOW),
            pageable
        );

        return PageResponseUtil.createPageResponse(
            appointmentMapper.toAppointmentResponsePage(pageResult)
        );
    }

    @Cacheable(value = "appointments", key = "'patient_' + #patientId + '_page_' + #page + '_' + #size")
    @Transactional(readOnly = true)
    public PageResponse<AppointmentResponse> getPatientAppointments(Long patientId, int page, int size, String sortBy, String sortDir) {
        Sort sort = createSort(sortBy, sortDir);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Appointment> pageResult = appointmentRepository.findByPatientIdAndStatusNotIn(
            patientId,
            List.of(AppointmentStatus.CANCELLED, AppointmentStatus.NO_SHOW),
            pageable
        );

        return PageResponseUtil.createPageResponse(
            appointmentMapper.toAppointmentResponsePage(pageResult)
        );
    }

    // ==================== PRIVATE HELPERS ====================

    private User findDoctor(Long doctorId) {
        return userRepository.findById(doctorId)
            .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + doctorId));
    }

    private User findPatient(Long patientId) {
        return userRepository.findById(patientId)
            .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + patientId));
    }

    private Sort createSort(String sortBy, String sortDir) {
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(direction, sortBy);
    }
}

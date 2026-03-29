package com.erandevu.service;

import com.erandevu.dto.request.AppointmentRequest;
import com.erandevu.dto.response.AppointmentResponse;
import com.erandevu.dto.response.PageResponse;
import com.erandevu.entity.Appointment;
import com.erandevu.entity.User;
import com.erandevu.enums.AppointmentStatus;
import com.erandevu.exception.BusinessRuleException;
import com.erandevu.exception.ResourceNotFoundException;
import com.erandevu.mapper.AppointmentMapper;
import com.erandevu.repository.AppointmentRepository;
import com.erandevu.repository.ScheduleRepository;
import com.erandevu.repository.UserRepository;
import com.erandevu.util.PageResponseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EnhancedAppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final ScheduleRepository scheduleRepository;
    private final AppointmentMapper appointmentMapper;
    private final AppointmentValidationService validationService;

    /**
     * Creates a new appointment with comprehensive business rule validation
     */
    @CacheEvict(value = "appointments", allEntries = true)
    public AppointmentResponse createAppointment(AppointmentRequest request) {
        log.info("Creating appointment for doctor: {}, patient: {}", 
                 request.getDoctorId(), request.getPatientId());

        // Find doctor and patient
        User doctor = userRepository.findByIdAndEnabledTrue(request.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));
        User patient = userRepository.findByIdAndEnabledTrue(request.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

        // Create appointment entity
        Appointment appointment = appointmentMapper.toAppointment(request, doctor, patient);
        
        // Validate against all business rules
        validationService.validateAppointmentCreation(appointment);

        // Save appointment
        Appointment savedAppointment = appointmentRepository.save(appointment);
        
        log.info("Appointment created successfully with ID: {}", savedAppointment.getId());
        return appointmentMapper.toAppointmentResponse(savedAppointment);
    }

    /**
     * Updates an existing appointment with validation
     */
    @CacheEvict(value = "appointments", allEntries = true)
    public AppointmentResponse updateAppointment(Long id, AppointmentRequest request) {
        log.info("Updating appointment: {}", id);

        Appointment existingAppointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        // Update appointment details
        appointmentMapper.updateAppointmentFromRequest(request, existingAppointment);

        // Validate updated appointment
        validationService.validateAppointmentModification(existingAppointment, existingAppointment);

        Appointment updatedAppointment = appointmentRepository.save(existingAppointment);
        
        log.info("Appointment updated successfully: {}", updatedAppointment.getId());
        return appointmentMapper.toAppointmentResponse(updatedAppointment);
    }

    /**
     * Cancels an appointment with business rule validation
     */
    @CacheEvict(value = "appointments", allEntries = true)
    public void cancelAppointment(Long id) {
        log.info("Cancelling appointment: {}", id);

        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        // Validate cancellation
        validationService.validateAppointmentCancellation(appointment);

        // Update appointment status
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancellationReason("Cancelled by patient");
        
        appointmentRepository.save(appointment);
        
        log.info("Appointment cancelled successfully: {}", id);
    }

    /**
     * Gets appointment by ID with caching
     */
    @Cacheable(value = "appointments", key = "#id")
    public AppointmentResponse getAppointmentById(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
        
        return appointmentMapper.toAppointmentResponse(appointment);
    }

    /**
     * Gets appointments for a doctor with date filtering
     */
    @Cacheable(value = "doctorAppointments", key = "#doctorId + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public PageResponse<AppointmentResponse> getDoctorAppointments(
            Long doctorId, 
            LocalDateTime startDate, 
            LocalDateTime endDate, 
            Pageable pageable
    ) {
        // Verify doctor exists
        userRepository.findByIdAndEnabledTrue(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

        Page<Appointment> appointments;
        
        if (startDate != null && endDate != null) {
            appointments = appointmentRepository.findByDoctorIdAndAppointmentDateTimeBetween(
                    doctorId, startDate, endDate, pageable
            );
        } else {
            appointments = appointmentRepository.findAll(pageable);
        }

        return PageResponseUtil.create(appointments.map(appointmentMapper::toAppointmentResponse));
    }

    /**
     * Gets today's appointments for a doctor
     */
    @Cacheable(value = "todayAppointments", key = "#doctorId")
    public List<AppointmentResponse> getTodayAppointments(Long doctorId) {
        List<Appointment> appointments = appointmentRepository.findTodayAppointmentsByDoctorId(doctorId);
        return appointments.stream()
                .map(appointmentMapper::toAppointmentResponse)
                .toList();
    }

    /**
     * Gets patient's upcoming appointments
     */
    @Cacheable(value = "patientAppointments", key = "#patientId + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public PageResponse<AppointmentResponse> getPatientAppointments(
            Long patientId, 
            Pageable pageable
    ) {
        // Verify patient exists
        userRepository.findByIdAndEnabledTrue(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

        Page<Appointment> appointments = appointmentRepository
                .findByPatientIdAndAppointmentDateTimeAfterAndIsDeletedFalseOrderByAppointmentDateTime(
                        patientId, LocalDateTime.now(), pageable
                );

        return PageResponseUtil.create(appointments.map(appointmentMapper::toAppointmentResponse));
    }

    /**
     * Gets appointments by status with pagination
     */
    @Cacheable(value = "appointmentsByStatus", key = "#status + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public PageResponse<AppointmentResponse> getAppointmentsByStatus(
            AppointmentStatus status, 
            Pageable pageable
    ) {
        Page<Appointment> appointments = appointmentRepository
                .findByStatusAndIsDeletedFalse(status, pageable);

        return PageResponseUtil.create(appointments.map(appointmentMapper::toAppointmentResponse));
    }

    /**
     * Gets appointment statistics for a doctor
     */
    public AppointmentStatistics getDoctorAppointmentStatistics(Long doctorId, LocalDateTime startDate, LocalDateTime endDate) {
        // Verify doctor exists
        userRepository.findByIdAndEnabledTrue(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

        List<Appointment> appointments = appointmentRepository
                .findByDoctorIdAndAppointmentDateTimeBetween(doctorId, startDate, endDate, null)
                .getContent();

        return AppointmentStatistics.builder()
                .doctorId(doctorId)
                .totalAppointments(appointments.size())
                .completedAppointments((int) appointments.stream()
                        .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED)
                        .count())
                .cancelledAppointments((int) appointments.stream()
                        .filter(a -> a.getStatus() == AppointmentStatus.CANCELLED)
                        .count())
                .scheduledAppointments((int) appointments.stream()
                        .filter(a -> a.getStatus() == AppointmentStatus.SCHEDULED)
                        .count())
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }

    /**
     * Appointment statistics DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class AppointmentStatistics {
        private Long doctorId;
        private Integer totalAppointments;
        private Integer completedAppointments;
        private Integer cancelledAppointments;
        private Integer scheduledAppointments;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
    }
}

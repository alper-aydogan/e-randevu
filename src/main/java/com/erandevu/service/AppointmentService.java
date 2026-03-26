package com.erandevu.service;

import com.erandevu.dto.request.AppointmentRequest;
import com.erandevu.dto.response.AppointmentResponse;
import com.erandevu.dto.response.PageResponse;
import com.erandevu.entity.Appointment;
import com.erandevu.entity.User;
import com.erandevu.enums.AppointmentStatus;
import com.erandevu.exception.AppointmentConflictException;
import com.erandevu.exception.InvalidAppointmentTimeException;
import com.erandevu.exception.ResourceNotFoundException;
import com.erandevu.mapper.AppointmentMapper;
import com.erandevu.repository.AppointmentRepository;
import com.erandevu.repository.UserRepository;
import com.erandevu.util.PageResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final AppointmentMapper appointmentMapper;

    public AppointmentService(AppointmentRepository appointmentRepository, UserRepository userRepository, 
                            AppointmentMapper appointmentMapper) {
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
        this.appointmentMapper = appointmentMapper;
    }

    public AppointmentResponse createAppointment(AppointmentRequest request) {
        // Validate appointment time is in future
        if (request.getAppointmentDateTime().isBefore(LocalDateTime.now())) {
            throw new InvalidAppointmentTimeException("Appointment time must be in the future");
        }

        // Get doctor and patient
        User doctor = userRepository.findByIdAndEnabledTrue(request.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id: " + request.getDoctorId()));
        
        User patient = userRepository.findByIdAndEnabledTrue(request.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + request.getPatientId()));

        // Check for appointment conflicts
        List<Appointment> conflictingAppointments = appointmentRepository.findConflictingAppointments(
                request.getDoctorId(),
                request.getAppointmentDateTime(),
                request.getAppointmentDateTime().plusMinutes(30)
        );

        if (!conflictingAppointments.isEmpty()) {
            throw new AppointmentConflictException(
                    "Doctor already has an appointment at this time: " + request.getAppointmentDateTime()
            );
        }

        // Create appointment
        Appointment appointment = new Appointment();
        appointment.setDoctor(doctor);
        appointment.setPatient(patient);
        appointment.setAppointmentDateTime(request.getAppointmentDateTime());
        appointment.setNotes(request.getNotes());
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        Appointment savedAppointment = appointmentRepository.save(appointment);

        return appointmentMapper.toAppointmentResponse(savedAppointment);
    }

    public AppointmentResponse getAppointmentById(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id));
        return appointmentMapper.toAppointmentResponse(appointment);
    }

    public List<AppointmentResponse> getAppointmentsByDoctor(Long doctorId) {
        // Verify doctor exists
        userRepository.findByIdAndEnabledTrue(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id: " + doctorId));

        List<Appointment> appointments = appointmentRepository.findByDoctorIdAndStatusNotIn(
                doctorId,
                List.of(AppointmentStatus.CANCELLED, AppointmentStatus.NO_SHOW)
        );

        return appointments.stream()
                .map(appointmentMapper::toAppointmentResponse)
                .toList();
    }

    public PageResponse<AppointmentResponse> getAppointmentsByDoctorPaginated(Long doctorId, int page, int size, String sortBy, String sortDir) {
        // Verify doctor exists
        userRepository.findByIdAndEnabledTrue(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id: " + doctorId));

        Pageable pageable = PageRequest.of(page, size);
        Page<Appointment> appointmentPage = appointmentRepository.findByDoctorIdAndStatusNotIn(
                doctorId,
                List.of(AppointmentStatus.CANCELLED, AppointmentStatus.NO_SHOW),
                pageable
        );
        Page<AppointmentResponse> appointmentResponsePage = appointmentMapper.toAppointmentResponsePage(appointmentPage);
        return PageResponseUtil.createPageResponse(appointmentResponsePage);
    }

    public List<AppointmentResponse> getAppointmentsByPatient(Long patientId) {
        // Verify patient exists
        userRepository.findByIdAndEnabledTrue(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + patientId));

        List<Appointment> appointments = appointmentRepository.findByPatientIdAndStatusNotIn(
                patientId,
                List.of(AppointmentStatus.CANCELLED, AppointmentStatus.NO_SHOW)
        );

        return appointments.stream()
                .map(appointmentMapper::toAppointmentResponse)
                .toList();
    }

    public PageResponse<AppointmentResponse> getAppointmentsByPatientPaginated(Long patientId, int page, int size, String sortBy, String sortDir) {
        // Verify patient exists
        userRepository.findByIdAndEnabledTrue(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + patientId));

        Pageable pageable = PageRequest.of(page, size);
        Page<Appointment> appointmentPage = appointmentRepository.findByPatientIdAndStatusNotIn(
                patientId,
                List.of(AppointmentStatus.CANCELLED, AppointmentStatus.NO_SHOW),
                pageable
        );
        Page<AppointmentResponse> appointmentResponsePage = appointmentMapper.toAppointmentResponsePage(appointmentPage);
        return PageResponseUtil.createPageResponse(appointmentResponsePage);
    }

    public AppointmentResponse cancelAppointment(Long id, String cancellationReason) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id));

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new InvalidAppointmentTimeException("Appointment is already cancelled");
        }

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new InvalidAppointmentTimeException("Cannot cancel completed appointment");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancellationReason(cancellationReason);

        Appointment updatedAppointment = appointmentRepository.save(appointment);
        return appointmentMapper.toAppointmentResponse(updatedAppointment);
    }

    public void deleteAppointment(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id));

        appointmentRepository.delete(appointment);
    }

    }

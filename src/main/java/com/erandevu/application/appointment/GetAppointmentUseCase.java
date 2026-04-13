package com.erandevu.application.appointment;

import com.erandevu.dto.response.AppointmentResponse;
import com.erandevu.entity.Appointment;
import com.erandevu.exception.ResourceNotFoundException;
import com.erandevu.mapper.AppointmentMapper;
import com.erandevu.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application Use Case: Get Appointment by ID (Query).
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class GetAppointmentUseCase {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentMapper appointmentMapper;

    @Cacheable(value = "appointments", key = "'apt_' + #id")
    public AppointmentResponse execute(Long id) {
        log.debug("UC: Fetching appointment - id={}", id);

        return appointmentRepository.findById(id)
            .map(appointmentMapper::toAppointmentResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + id));
    }
}

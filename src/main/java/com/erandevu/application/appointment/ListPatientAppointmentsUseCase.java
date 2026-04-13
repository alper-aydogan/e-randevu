package com.erandevu.application.appointment;

import com.erandevu.dto.response.AppointmentResponse;
import com.erandevu.dto.response.PageResponse;
import com.erandevu.entity.Appointment;
import com.erandevu.enums.AppointmentStatus;
import com.erandevu.mapper.AppointmentMapper;
import com.erandevu.repository.AppointmentRepository;
import com.erandevu.util.PageResponseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Application Use Case: List Patient's Appointments (Query).
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ListPatientAppointmentsUseCase {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentMapper appointmentMapper;

    @Cacheable(value = "appointments", key = "'patient_' + #patientId + '_page_' + #page + '_' + #size + '_' + #sortBy + '_' + #sortDir")
    public PageResponse<AppointmentResponse> execute(Long patientId, int page, int size, String sortBy, String sortDir) {
        log.debug("UC: Listing patient appointments - patientId={}, page={}", patientId, page);

        Sort sort = Sort.by(sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy);
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
}

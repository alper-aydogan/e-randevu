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
import com.erandevu.util.PageResponseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Randevu servisi - Sadece orkestrasyon mantığı içerir.
 * Tüm validasyonlar AppointmentValidationService'e devredilir.
 * Transaction ve caching yönetimi burada yapılır.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentValidationService validationService;
    private final AppointmentMapper appointmentMapper;

    /**
     * Yeni randevu oluşturur.
     * Tüm validasyonlar AppointmentValidationService tarafından yapılır.
     *
     * @param request randevu isteği
     * @return oluşturulan randevu yanıtı
     * @throws AppointmentConflictException çakışma durumunda
     */
    @CacheEvict(value = "appointments", allEntries = true)
    public AppointmentResponse createAppointment(AppointmentRequest request) {
        log.info("Creating appointment: doctor={}, patient={}, time={}",
                request.getDoctorId(), request.getPatientId(), request.getAppointmentDateTime());

        try {
            // 1. Validasyon (Validation Service)
            validationService.validateAppointmentCreation(
                    request,
                    request.getDoctorId(),
                    request.getPatientId()
            );

            // 2. Doktor ve hasta getir
            User doctor = validationService.validateDoctorExists(request.getDoctorId());
            User patient = validationService.validatePatientExists(request.getPatientId());

            // 3. Entity oluştur
            Appointment appointment = Appointment.builder()
                    .doctor(doctor)
                    .patient(patient)
                    .appointmentDateTime(request.getAppointmentDateTime())
                    .notes(request.getNotes())
                    .status(AppointmentStatus.SCHEDULED)
                    .build();

            // 4. Kaydet
            Appointment savedAppointment = appointmentRepository.save(appointment);
            log.info("Appointment created successfully: id={}", savedAppointment.getId());

            return appointmentMapper.toAppointmentResponse(savedAppointment);

        } catch (DataIntegrityViolationException e) {
            log.warn("Concurrent booking detected: doctor={}, time={}",
                    request.getDoctorId(), request.getAppointmentDateTime());
            throw new AppointmentConflictException(
                    "This appointment slot is already booked. Please choose a different time.",
                    request.getDoctorId(),
                    request.getAppointmentDateTime()
            );
        } catch (ObjectOptimisticLockingFailureException e) {
            log.warn("Optimistic locking failure during appointment creation");
            throw new AppointmentConflictException(
                    "Appointment slot is currently being booked by another user. Please try again.",
                    request.getDoctorId(),
                    request.getAppointmentDateTime()
            );
        }
    }

    @Cacheable(value = "appointments", key = "#id")
    public AppointmentResponse getAppointmentById(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id));
        return appointmentMapper.toAppointmentResponse(appointment);
    }

    /**
     * Doktora ait randevuları getirir.
     *
     * @param doctorId doktor ID
     * @return randevu listesi
     */
    @Cacheable(value = "appointments", key = "'doctor_' + #doctorId")
    public List<AppointmentResponse> getAppointmentsByDoctor(Long doctorId) {
        validationService.validateDoctorExists(doctorId);

        List<Appointment> appointments = appointmentRepository.findByDoctorIdAndStatusNotIn(
                doctorId,
                List.of(AppointmentStatus.CANCELLED, AppointmentStatus.NO_SHOW)
        );

        return appointments.stream()
                .map(appointmentMapper::toAppointmentResponse)
                .toList();
    }

    /**
     * Doktora ait randevuları sayfalama ile getirir.
     *
     * @param doctorId doktor ID
     * @param page sayfa numarası
     * @param size sayfa boyutu
     * @return sayfalanmış randevu yanıtı
     */
    @Cacheable(value = "appointments", key = "'doctor_paged_' + #doctorId + '_' + #page + '_' + #size")
    public PageResponse<AppointmentResponse> getAppointmentsByDoctorPaginated(Long doctorId, int page, int size, String sortBy, String sortDir) {
        validationService.validateDoctorExists(doctorId);

        Pageable pageable = PageRequest.of(page, size);
        Page<Appointment> appointmentPage = appointmentRepository.findByDoctorIdAndStatusNotIn(
                doctorId,
                List.of(AppointmentStatus.CANCELLED, AppointmentStatus.NO_SHOW),
                pageable
        );
        Page<AppointmentResponse> appointmentResponsePage = appointmentMapper.toAppointmentResponsePage(appointmentPage);
        return PageResponseUtil.createPageResponse(appointmentResponsePage);
    }

    /**
     * Hastaya ait randevuları getirir.
     *
     * @param patientId hasta ID
     * @return randevu listesi
     */
    @Cacheable(value = "appointments", key = "'patient_' + #patientId")
    public List<AppointmentResponse> getAppointmentsByPatient(Long patientId) {
        validationService.validatePatientExists(patientId);

        List<Appointment> appointments = appointmentRepository.findByPatientIdAndStatusNotIn(
                patientId,
                List.of(AppointmentStatus.CANCELLED, AppointmentStatus.NO_SHOW)
        );

        return appointments.stream()
                .map(appointmentMapper::toAppointmentResponse)
                .toList();
    }

    /**
     * Hastaya ait randevuları sayfalama ile getirir.
     *
     * @param patientId hasta ID
     * @param page sayfa numarası
     * @param size sayfa boyutu
     * @return sayfalanmış randevu yanıtı
     */
    @Cacheable(value = "appointments", key = "'patient_paged_' + #patientId + '_' + #page + '_' + #size")
    public PageResponse<AppointmentResponse> getAppointmentsByPatientPaginated(Long patientId, int page, int size, String sortBy, String sortDir) {
        validationService.validatePatientExists(patientId);

        Pageable pageable = PageRequest.of(page, size);
        Page<Appointment> appointmentPage = appointmentRepository.findByPatientIdAndStatusNotIn(
                patientId,
                List.of(AppointmentStatus.CANCELLED, AppointmentStatus.NO_SHOW),
                pageable
        );
        Page<AppointmentResponse> appointmentResponsePage = appointmentMapper.toAppointmentResponsePage(appointmentPage);
        return PageResponseUtil.createPageResponse(appointmentResponsePage);
    }

    /**
     * Randevuyu iptal eder.
     * Validasyon AppointmentValidationService tarafından yapılır.
     *
     * @param id randevu ID
     * @param cancellationReason iptal nedeni
     * @return güncellenmiş randevu
     */
    @CacheEvict(value = "appointments", allEntries = true)
    public AppointmentResponse cancelAppointment(Long id, String cancellationReason) {
        log.info("Cancelling appointment: id={}", id);

        Appointment appointment = appointmentRepository.findByIdWithLock(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id));

        // Validasyon
        validationService.validateAppointmentCancellation(appointment);

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancellationReason(cancellationReason);
        Appointment updatedAppointment = appointmentRepository.save(appointment);

        log.info("Appointment cancelled successfully: id={}", id);
        return appointmentMapper.toAppointmentResponse(updatedAppointment);
    }

    /**
     * Randevuyu siler (soft delete).
     * Cache temizlenir.
     *
     * @param id randevu ID
     */
    @CacheEvict(value = "appointments", allEntries = true)
    public void deleteAppointment(Long id) {
        log.info("Deleting appointment: id={}", id);

        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id));

        appointmentRepository.delete(appointment);
        log.info("Appointment deleted successfully: id={}", id);
    }

    /**
     * Randevu günceller.
     * Validasyon AppointmentValidationService tarafından yapılır.
     *
     * @param id randevu ID
     * @param request güncelleme isteği
     * @return güncellenmiş randevu
     */
    @CacheEvict(value = "appointments", allEntries = true)
    public AppointmentResponse updateAppointment(Long id, AppointmentRequest request) {
        log.info("Updating appointment: id={}", id);

        Appointment existingAppointment = appointmentRepository.findByIdWithLock(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id));

        // Validasyon
        validationService.validateAppointmentUpdate(existingAppointment, request);

        // Güncelle
        existingAppointment.setAppointmentDateTime(request.getAppointmentDateTime());
        existingAppointment.setNotes(request.getNotes());

        Appointment updatedAppointment = appointmentRepository.save(existingAppointment);
        log.info("Appointment updated successfully: id={}", id);

        return appointmentMapper.toAppointmentResponse(updatedAppointment);
    }
}

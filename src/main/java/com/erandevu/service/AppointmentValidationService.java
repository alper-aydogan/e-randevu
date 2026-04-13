package com.erandevu.service;

import com.erandevu.dto.request.AppointmentRequest;
import com.erandevu.entity.Appointment;
import com.erandevu.entity.Schedule;
import com.erandevu.entity.User;
import com.erandevu.enums.AppointmentStatus;
import com.erandevu.exception.AppointmentConflictException;
import com.erandevu.exception.BusinessRuleException;
import com.erandevu.exception.InvalidAppointmentTimeException;
import com.erandevu.exception.ResourceNotFoundException;
import com.erandevu.repository.AppointmentRepository;
import com.erandevu.repository.ScheduleRepository;
import com.erandevu.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Merkezi randevu doğrulama servisi.
 * Tüm iş kuralları ve validasyonlar burada tek bir yerde yönetilir.
 * Tekrar eden validasyon mantığı yoktur.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AppointmentValidationService {

    private final UserRepository userRepository;
    private final ScheduleRepository scheduleRepository;
    private final AppointmentRepository appointmentRepository;

    // Sabitler - Validasyon kuralları
    public static final int MIN_ADVANCE_BOOKING_HOURS = 2;
    public static final int MAX_BOOKING_DAYS_AHEAD = 30;
    public static final int MAX_APPOINTMENTS_PER_DOCTOR_PER_DAY = 8;
    public static final int DEFAULT_APPOINTMENT_DURATION_MINUTES = 30;

    /**
     * Randevu oluşturma için tüm validasyonları çalıştırır.
     * Bu metod TÜM iş kurallarını tek seferde kontrol eder.
     *
     * @param request randevu isteği
     * @param doctorId doktor ID
     * @param patientId hasta ID
     * @throws BusinessRuleException iş kuralı ihlali
     * @throws AppointmentConflictException çakışma durumu
     * @throws InvalidAppointmentTimeException geçersiz zaman
     */
    public void validateAppointmentCreation(AppointmentRequest request, Long doctorId, Long patientId) {
        log.debug("Validating appointment creation: doctor={}, patient={}, time={}",
                doctorId, patientId, request.getAppointmentDateTime());

        // 1. Temel zaman validasyonları
        validateAppointmentTimeConstraints(request.getAppointmentDateTime());

        // 2. Doktor ve hasta aynı olamaz
        validateDoctorIsNotPatient(doctorId, patientId);

        // 3. Doktor müsaitlik kontrolü
        validateDoctorAvailability(doctorId, request.getAppointmentDateTime());

        // 4. Çakışma kontrolü
        validateNoAppointmentConflicts(doctorId, request.getAppointmentDateTime());

        // 5. Günlük limit kontrolü
        validateDailyAppointmentLimit(doctorId, request.getAppointmentDateTime());

        log.debug("Appointment validation passed for doctor={}", doctorId);
    }

    /**
     * Randevu güncelleme için validasyon.
     *
     * @param existingAppointment mevcut randevu
     * @param request yeni randevu isteği
     */
    public void validateAppointmentUpdate(Appointment existingAppointment, AppointmentRequest request) {
        log.debug("Validating appointment update: id={}", existingAppointment.getId());

        // Sadece planlanmış randevular güncellenebilir
        if (existingAppointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new BusinessRuleException(
                    "Only scheduled appointments can be modified. Current status: " + existingAppointment.getStatus()
            );
        }

        // Yeni zaman için tüm validasyonları çalıştır
        validateAppointmentTimeConstraints(request.getAppointmentDateTime());
        validateDoctorAvailability(request.getDoctorId(), request.getAppointmentDateTime());
        validateNoAppointmentConflicts(request.getDoctorId(), request.getAppointmentDateTime(), existingAppointment.getId());
        validateDailyAppointmentLimit(request.getDoctorId(), request.getAppointmentDateTime());
    }

    /**
     * Randevu iptali için validasyon.
     *
     * @param appointment iptal edilecek randevu
     */
    public void validateAppointmentCancellation(Appointment appointment) {
        log.debug("Validating appointment cancellation: id={}", appointment.getId());

        // Zaten iptal edilmiş kontrolü
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new BusinessRuleException("Appointment is already cancelled");
        }

        // Tamamlanmış randevu kontrolü
        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new BusinessRuleException("Cannot cancel completed appointment");
        }

        // Minimum süre kontrolü (2 saat öncesi)
        LocalDateTime appointmentTime = appointment.getAppointmentDateTime();
        if (appointmentTime.isBefore(LocalDateTime.now().plusHours(MIN_ADVANCE_BOOKING_HOURS))) {
            throw new BusinessRuleException(
                    "Cannot cancel appointments less than " + MIN_ADVANCE_BOOKING_HOURS + " hours before scheduled time"
            );
        }
    }

    /**
     * Temel randevu zaman validasyonları.
     * TEK bir yerde implemente edilir.
     *
     * @param appointmentDateTime randevu zamanı
     */
    public void validateAppointmentTimeConstraints(LocalDateTime appointmentDateTime) {
        LocalDateTime now = LocalDateTime.now();

        // Geçmiş zaman kontrolü
        if (appointmentDateTime.isBefore(now)) {
            throw new InvalidAppointmentTimeException("Appointment time must be in the future");
        }

        // Minimum önceden rezervasyon (2 saat)
        if (appointmentDateTime.isBefore(now.plusHours(MIN_ADVANCE_BOOKING_HOURS))) {
            throw new InvalidAppointmentTimeException(
                    "Appointments must be booked at least " + MIN_ADVANCE_BOOKING_HOURS + " hours in advance"
            );
        }

        // Maksimum ileri tarih (30 gün)
        if (appointmentDateTime.isAfter(now.plusDays(MAX_BOOKING_DAYS_AHEAD))) {
            throw new InvalidAppointmentTimeException(
                    "Appointments cannot be booked more than " + MAX_BOOKING_DAYS_AHEAD + " days in advance"
            );
        }

        // Hafta sonu kontrolü
        DayOfWeek dayOfWeek = appointmentDateTime.getDayOfWeek();
        if (isWeekend(dayOfWeek)) {
            throw new InvalidAppointmentTimeException("Appointments are not available on weekends");
        }

        // İş saatleri kontrolü (09:00 - 18:00)
        LocalTime appointmentTime = appointmentDateTime.toLocalTime();
        if (appointmentTime.isBefore(LocalTime.of(9, 0)) || appointmentTime.isAfter(LocalTime.of(18, 0))) {
            throw new InvalidAppointmentTimeException("Appointments are only available between 9:00 AM and 6:00 PM");
        }
    }

    /**
     * Doktor ve hasta aynı kişi olamaz validasyonu.
     */
    private void validateDoctorIsNotPatient(Long doctorId, Long patientId) {
        if (doctorId.equals(patientId)) {
            throw new BusinessRuleException("Doctor and patient cannot be the same person");
        }
    }

    /**
     * Doktor müsaitlik kontrolü.
     * Hem çalışma takvimi hem de izin günleri kontrol edilir.
     */
    public void validateDoctorAvailability(Long doctorId, LocalDateTime appointmentDateTime) {
        DayOfWeek dayOfWeek = appointmentDateTime.getDayOfWeek();

        // Doktorun çalışma takvimi var mı?
        Schedule schedule = scheduleRepository
                .findByDoctorIdAndDayOfWeekAndActiveTrue(doctorId, dayOfWeek)
                .orElseThrow(() -> new BusinessRuleException(
                        "Doctor is not available on " + dayOfWeek + ". No schedule found."
                ));

        // Randevu saati çalışma saatleri içinde mi?
        LocalTime appointmentTime = appointmentDateTime.toLocalTime();
        if (appointmentTime.isBefore(schedule.getStartTime()) ||
                appointmentTime.isAfter(schedule.getEndTime())) {
            throw new BusinessRuleException(
                    "Appointment time is outside doctor's working hours. " +
                            "Available: " + schedule.getStartTime() + " - " + schedule.getEndTime()
            );
        }

        // Randevu süresi çalışma saatlerine sığar mı?
        LocalDateTime appointmentEnd = appointmentDateTime.plusMinutes(
                schedule.getAppointmentDurationMinutes() != null ?
                        schedule.getAppointmentDurationMinutes() : DEFAULT_APPOINTMENT_DURATION_MINUTES
        );
        if (appointmentEnd.toLocalTime().isAfter(schedule.getEndTime())) {
            throw new BusinessRuleException("Appointment duration exceeds doctor's working hours");
        }
    }

    /**
     * Çakışan randevu kontrolü.
     * TEK bir yerde implemente edilir.
     */
    public void validateNoAppointmentConflicts(Long doctorId, LocalDateTime appointmentDateTime) {
        validateNoAppointmentConflicts(doctorId, appointmentDateTime, null);
    }

    /**
     * Çakışan randevu kontrolü (güncelleme için mevcut randevu hariç).
     */
    public void validateNoAppointmentConflicts(Long doctorId, LocalDateTime appointmentDateTime, Long excludeAppointmentId) {
        LocalDateTime appointmentEnd = appointmentDateTime.plusMinutes(DEFAULT_APPOINTMENT_DURATION_MINUTES);

        // Çakışma kontrolü - Repository katmanı pessimistic lock kullanır
        boolean hasConflicts = appointmentRepository.hasOverlappingAppointments(
                doctorId, appointmentDateTime, appointmentEnd
        );

        if (hasConflicts) {
            // Detaylı çakışma bilgisi için
            List<Appointment> conflicts = appointmentRepository.findOverlappingAppointments(
                    doctorId, appointmentDateTime, appointmentEnd
            );

            if (conflicts.stream().anyMatch(a -> !a.getId().equals(excludeAppointmentId))) {
                throw new AppointmentConflictException(
                        "Doctor already has an appointment at this time: " + appointmentDateTime,
                        doctorId,
                        appointmentDateTime
                );
            }
        }
    }

    /**
     * Günlük randevu limiti kontrolü.
     * Her doktor için maksimum 8 randevu.
     */
    public void validateDailyAppointmentLimit(Long doctorId, LocalDateTime appointmentDateTime) {
        LocalDate appointmentDate = appointmentDateTime.toLocalDate();
        LocalDateTime startOfDay = appointmentDate.atStartOfDay();

        long dailyCount = appointmentRepository.countDailyAppointments(doctorId, startOfDay);

        if (dailyCount >= MAX_APPOINTMENTS_PER_DOCTOR_PER_DAY) {
            throw new BusinessRuleException(
                    "Doctor has reached maximum daily appointment limit (" +
                            MAX_APPOINTMENTS_PER_DOCTOR_PER_DAY + ")"
            );
        }
    }

    /**
     * Hafta sonu kontrolü.
     */
    private boolean isWeekend(DayOfWeek dayOfWeek) {
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    /**
     * Doktor varlık kontrolü.
     */
    public User validateDoctorExists(Long doctorId) {
        return userRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id: " + doctorId));
    }

    /**
     * Hasta varlık kontrolü.
     */
    public User validatePatientExists(Long patientId) {
        return userRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + patientId));
    }
}

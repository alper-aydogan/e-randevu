package com.erandevu.service;

import com.erandevu.dto.request.AppointmentRequest;
import com.erandevu.dto.response.AppointmentResponse;
import com.erandevu.entity.Appointment;
import com.erandevu.entity.User;
import com.erandevu.enums.AppointmentStatus;
import com.erandevu.enums.Role;
import com.erandevu.exception.AppointmentConflictException;
import com.erandevu.exception.ResourceNotFoundException;
import com.erandevu.mapper.AppointmentMapper;
import com.erandevu.repository.AppointmentRepository;
import com.erandevu.repository.UserRepository;
import com.erandevu.service.validation.AppointmentValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Modernized AppointmentServiceTest aligned with Clean Architecture.
 * Tests the orchestration layer with proper mocking of validators and repositories.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Appointment Service Tests")
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AppointmentMapper appointmentMapper;

    @Mock
    private AppointmentValidator validator;

    @InjectMocks
    private AppointmentService appointmentService;

    private TestDataBuilder testData;

    @BeforeEach
    void setUp() {
        testData = new TestDataBuilder();
    }

    /**
     * Test data builder for clean test setup (Given/When/Then pattern)
     */
    private class TestDataBuilder {
        final LocalDateTime futureDateTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        final Long doctorId = 1L;
        final Long patientId = 2L;
        final Long appointmentId = 100L;

        User buildDoctor() {
            return User.builder()
                    .id(doctorId)
                    .username("dr_john")
                    .email("john@hospital.com")
                    .firstName("John")
                    .lastName("Smith")
                    .role(Role.DOCTOR)
                    .enabled(true)
                    .build();
        }

        User buildPatient() {
            return User.builder()
                    .id(patientId)
                    .username("patient_jane")
                    .email("jane@email.com")
                    .firstName("Jane")
                    .lastName("Doe")
                    .role(Role.PATIENT)
                    .enabled(true)
                    .build();
        }

        AppointmentRequest buildValidRequest() {
            return AppointmentRequest.builder()
                    .doctorId(doctorId)
                    .appointmentDateTime(futureDateTime)
                    .notes("Regular checkup")
                    .build();
        }

        Appointment buildScheduledAppointment(User doctor, User patient) {
            return Appointment.builder()
                    .id(appointmentId)
                    .doctor(doctor)
                    .patient(patient)
                    .appointmentDateTime(futureDateTime)
                    .notes("Regular checkup")
                    .status(AppointmentStatus.SCHEDULED)
                    .build();
        }

        AppointmentResponse buildResponse(Appointment appointment) {
            return AppointmentResponse.builder()
                    .id(appointment.getId())
                    .doctorId(appointment.getDoctor().getId())
                    .doctorName("Dr. " + appointment.getDoctor().getFirstName() + " " + appointment.getDoctor().getLastName())
                    .patientId(appointment.getPatient().getId())
                    .patientName(appointment.getPatient().getFirstName() + " " + appointment.getPatient().getLastName())
                    .appointmentDateTime(appointment.getAppointmentDateTime())
                    .notes(appointment.getNotes())
                    .status(appointment.getStatus())
                    .build();
        }
    }

    @Nested
    @DisplayName("Create Appointment Tests")
    class CreateAppointmentTests {

        @Test
        @DisplayName("Should create appointment successfully when valid data provided")
        void createAppointment_ValidData_ReturnsAppointmentResponse() {
            // Given
            User doctor = testData.buildDoctor();
            User patient = testData.buildPatient();
            AppointmentRequest request = testData.buildValidRequest();
            Appointment appointment = testData.buildScheduledAppointment(doctor, patient);
            AppointmentResponse expectedResponse = testData.buildResponse(appointment);

            when(userRepository.findById(doctor.getId())).thenReturn(Optional.of(doctor));
            when(userRepository.findById(patient.getId())).thenReturn(Optional.of(patient));
            when(appointmentRepository.save(any(Appointment.class))).thenReturn(appointment);
            when(appointmentMapper.toAppointmentResponse(appointment)).thenReturn(expectedResponse);

            // When
            AppointmentResponse actualResponse = appointmentService.createAppointment(request, patient.getId());

            // Then
            assertThat(actualResponse).isNotNull();
            assertThat(actualResponse.getId()).isEqualTo(appointment.getId());
            assertThat(actualResponse.getDoctorId()).isEqualTo(doctor.getId());
            assertThat(actualResponse.getPatientId()).isEqualTo(patient.getId());
            assertThat(actualResponse.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);

            verify(validator).validateCreation(request, doctor.getId(), patient.getId());
            verify(userRepository).findById(doctor.getId());
            verify(userRepository).findById(patient.getId());
            verify(appointmentRepository).save(any(Appointment.class));
            verify(appointmentMapper).toAppointmentResponse(appointment);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when doctor not found")
        void createAppointment_DoctorNotFound_ThrowsResourceNotFoundException() {
            // Given
            AppointmentRequest request = testData.buildValidRequest();
            when(userRepository.findById(request.getDoctorId())).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> appointmentService.createAppointment(request, testData.patientId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Doctor");

            verify(userRepository).findById(request.getDoctorId());
            verifyNoInteractions(appointmentRepository);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when patient not found")
        void createAppointment_PatientNotFound_ThrowsResourceNotFoundException() {
            // Given
            User doctor = testData.buildDoctor();
            AppointmentRequest request = testData.buildValidRequest();
            
            when(userRepository.findById(doctor.getId())).thenReturn(Optional.of(doctor));
            when(userRepository.findById(testData.patientId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> appointmentService.createAppointment(request, testData.patientId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Patient");

            verify(userRepository).findById(doctor.getId());
            verify(userRepository).findById(testData.patientId);
            verifyNoInteractions(appointmentRepository);
        }
    }

    @Nested
    @DisplayName("Cancel Appointment Tests")
    class CancelAppointmentTests {

        @Test
        @DisplayName("Should cancel appointment successfully when status is scheduled")
        void cancelAppointment_ScheduledStatus_SuccessfullyCancels() {
            // Given
            User doctor = testData.buildDoctor();
            User patient = testData.buildPatient();
            Appointment appointment = testData.buildScheduledAppointment(doctor, patient);
            Appointment cancelled = Appointment.builder()
                    .id(appointment.getId())
                    .doctor(doctor)
                    .patient(patient)
                    .appointmentDateTime(appointment.getAppointmentDateTime())
                    .notes(appointment.getNotes())
                    .status(AppointmentStatus.CANCELLED)
                    .cancellationReason("Patient requested")
                    .build();
            AppointmentResponse expectedResponse = testData.buildResponse(cancelled);
            expectedResponse = expectedResponse.toBuilder()
                    .status(AppointmentStatus.CANCELLED)
                    .cancellationReason("Patient requested")
                    .build();

            when(appointmentRepository.findByIdWithLock(appointment.getId())).thenReturn(Optional.of(appointment));
            when(appointmentRepository.save(any(Appointment.class))).thenReturn(cancelled);
            when(appointmentMapper.toAppointmentResponse(any(Appointment.class))).thenReturn(expectedResponse);

            // When
            AppointmentResponse actualResponse = appointmentService.cancelAppointment(appointment.getId(), "Patient requested");

            // Then
            assertThat(actualResponse).isNotNull();
            assertThat(actualResponse.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
            assertThat(actualResponse.getCancellationReason()).isEqualTo("Patient requested");

            verify(validator).validateCancellation(appointment);
            verify(appointmentRepository).findByIdWithLock(appointment.getId());
            verify(appointmentRepository).save(any(Appointment.class));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when appointment not found")
        void cancelAppointment_NotFound_ThrowsResourceNotFoundException() {
            // Given
            when(appointmentRepository.findByIdWithLock(testData.appointmentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> appointmentService.cancelAppointment(testData.appointmentId, "Reason"))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(appointmentRepository).findByIdWithLock(testData.appointmentId);
            verifyNoMoreInteractions(appointmentRepository);
        }
    }

    @Nested
    @DisplayName("Get Appointment Tests")
    class GetAppointmentTests {

        @Test
        @DisplayName("Should return appointment by ID when exists")
        void getAppointmentById_Exists_ReturnsAppointmentResponse() {
            // Given
            User doctor = testData.buildDoctor();
            User patient = testData.buildPatient();
            Appointment appointment = testData.buildScheduledAppointment(doctor, patient);
            AppointmentResponse expectedResponse = testData.buildResponse(appointment);

            when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));
            when(appointmentMapper.toAppointmentResponse(appointment)).thenReturn(expectedResponse);

            // When
            AppointmentResponse actualResponse = appointmentService.getAppointmentById(appointment.getId());

            // Then
            assertThat(actualResponse).isNotNull();
            assertThat(actualResponse.getId()).isEqualTo(appointment.getId());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when appointment not found")
        void getAppointmentById_NotFound_ThrowsResourceNotFoundException() {
            // Given
            when(appointmentRepository.findById(testData.appointmentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> appointmentService.getAppointmentById(testData.appointmentId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}

package com.erandevu.service;

import com.erandevu.dto.request.AppointmentRequest;
import com.erandevu.dto.response.AppointmentResponse;
import com.erandevu.entity.Appointment;
import com.erandevu.entity.User;
import com.erandevu.enums.AppointmentStatus;
import com.erandevu.exception.AppointmentConflictException;
import com.erandevu.exception.InvalidAppointmentTimeException;
import com.erandevu.exception.ResourceNotFoundException;
import com.erandevu.mapper.AppointmentMapper;
import com.erandevu.repository.AppointmentRepository;
import com.erandevu.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Appointment Service Tests")
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AppointmentMapper appointmentMapper;

    @InjectMocks
    private AppointmentService appointmentService;

    private User testDoctor;
    private User testPatient;
    private AppointmentRequest validAppointmentRequest;
    private LocalDateTime futureDateTime;
    private LocalDateTime pastDateTime;

    @BeforeEach
    void setUp() {
        futureDateTime = LocalDateTime.now().plusDays(1);
        pastDateTime = LocalDateTime.now().minusHours(1);

        testDoctor = User.builder()
                .id(1L)
                .username("dr_john")
                .email("john@hospital.com")
                .firstName("John")
                .lastName("Smith")
                .role(com.erandevu.enums.Role.DOCTOR)
                .enabled(true)
                .build();

        testPatient = User.builder()
                .id(2L)
                .username("patient_jane")
                .email("jane@email.com")
                .firstName("Jane")
                .lastName("Doe")
                .role(com.erandevu.enums.Role.PATIENT)
                .enabled(true)
                .build();

        validAppointmentRequest = AppointmentRequest.builder()
                .doctorId(1L)
                .patientId(2L)
                .appointmentDateTime(futureDateTime)
                .notes("Regular checkup")
                .build();
    }

    @Test
    @DisplayName("Should create appointment successfully when valid data provided")
    void createAppointment_ValidData_ReturnsAppointmentResponse() {
        // Arrange
        when(userRepository.findByIdAndEnabledTrue(1L)).thenReturn(Optional.of(testDoctor));
        when(userRepository.findByIdAndEnabledTrue(2L)).thenReturn(Optional.of(testPatient));
        when(appointmentRepository.findConflictingAppointments(anyLong(), any(), any())).thenReturn(List.of());
        
        Appointment savedAppointment = Appointment.builder()
                .id(100L)
                .doctor(testDoctor)
                .patient(testPatient)
                .appointmentDateTime(futureDateTime)
                .notes("Regular checkup")
                .status(AppointmentStatus.SCHEDULED)
                .build();
        
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(savedAppointment);
        
        AppointmentResponse expectedResponse = AppointmentResponse.builder()
                .id(100L)
                .doctorId(1L)
                .doctorName("Dr. John Smith")
                .patientId(2L)
                .patientName("Jane Doe")
                .appointmentDateTime(futureDateTime)
                .notes("Regular checkup")
                .status(AppointmentStatus.SCHEDULED)
                .build();
        
        when(appointmentMapper.toAppointmentResponse(savedAppointment)).thenReturn(expectedResponse);

        // Act
        AppointmentResponse actualResponse = appointmentService.createAppointment(validAppointmentRequest);
        
        // Debug
        System.out.println("Actual Response: " + actualResponse);

        // Assert
        assertNotNull(actualResponse);
        assertEquals(100L, actualResponse.getId());
        assertEquals(1L, actualResponse.getDoctorId());
        assertEquals(2L, actualResponse.getPatientId());
        assertEquals("Dr. John Smith", actualResponse.getDoctorName());
        assertEquals("Jane Doe", actualResponse.getPatientName());
        assertEquals("Regular checkup", actualResponse.getNotes());
        assertEquals(AppointmentStatus.SCHEDULED, actualResponse.getStatus());

        verify(userRepository).findByIdAndEnabledTrue(1L);
        verify(userRepository).findByIdAndEnabledTrue(2L);
        verify(appointmentRepository).findConflictingAppointments(eq(1L), any(), any());
        verify(appointmentRepository).save(any(Appointment.class));
        verify(appointmentMapper).toAppointmentResponse(savedAppointment);
    }

    @Test
    @DisplayName("Should throw AppointmentConflictException when conflicting appointments exist")
    void createAppointment_ConflictingAppointments_ThrowsAppointmentConflictException() {
        // Arrange
        when(userRepository.findByIdAndEnabledTrue(1L)).thenReturn(Optional.of(testDoctor));
        when(userRepository.findByIdAndEnabledTrue(2L)).thenReturn(Optional.of(testPatient));
        
        Appointment conflictingAppointment = Appointment.builder()
                .id(50L)
                .appointmentDateTime(futureDateTime)
                .build();
        
        when(appointmentRepository.findConflictingAppointments(anyLong(), any(), any()))
                .thenReturn(List.of(conflictingAppointment));

        // Act & Assert
        assertThrows(AppointmentConflictException.class, 
                () -> appointmentService.createAppointment(validAppointmentRequest));

        verify(userRepository).findByIdAndEnabledTrue(1L);
        verify(userRepository).findByIdAndEnabledTrue(2L);
        verify(appointmentRepository).findConflictingAppointments(eq(1L), any(), any());
        verify(appointmentRepository, never()).save(any(Appointment.class));
        verify(appointmentMapper, never()).toAppointmentResponse(any());
    }

    @Test
    @DisplayName("Should throw InvalidAppointmentTimeException when appointment time is in past")
    void createAppointment_PastDateTime_ThrowsInvalidAppointmentTimeException() {
        // Arrange
        AppointmentRequest pastAppointmentRequest = AppointmentRequest.builder()
                .doctorId(1L)
                .patientId(2L)
                .appointmentDateTime(pastDateTime)
                .notes("Past appointment")
                .build();

        // Act & Assert
        assertThrows(InvalidAppointmentTimeException.class, 
                () -> appointmentService.createAppointment(pastAppointmentRequest));

        verify(userRepository, never()).findByIdAndEnabledTrue(anyLong());
        verify(appointmentRepository, never()).findConflictingAppointments(anyLong(), any(), any());
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when doctor not found")
    void createAppointment_DoctorNotFound_ThrowsResourceNotFoundException() {
        // Arrange
        when(userRepository.findByIdAndEnabledTrue(999L)).thenReturn(Optional.empty());

        AppointmentRequest invalidDoctorRequest = AppointmentRequest.builder()
                .doctorId(999L)
                .patientId(2L)
                .appointmentDateTime(futureDateTime)
                .notes("Invalid doctor")
                .build();

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, 
                () -> appointmentService.createAppointment(invalidDoctorRequest));

        verify(userRepository).findByIdAndEnabledTrue(999L);
        verify(userRepository, never()).findByIdAndEnabledTrue(2L);
        verify(appointmentRepository, never()).findConflictingAppointments(anyLong(), any(), any());
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when patient not found")
    void createAppointment_PatientNotFound_ThrowsResourceNotFoundException() {
        // Arrange
        when(userRepository.findByIdAndEnabledTrue(1L)).thenReturn(Optional.of(testDoctor));
        when(userRepository.findByIdAndEnabledTrue(888L)).thenReturn(Optional.empty());

        AppointmentRequest invalidPatientRequest = AppointmentRequest.builder()
                .doctorId(1L)
                .patientId(888L)
                .appointmentDateTime(futureDateTime)
                .notes("Invalid patient")
                .build();

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, 
                () -> appointmentService.createAppointment(invalidPatientRequest));

        verify(userRepository).findByIdAndEnabledTrue(1L);
        verify(userRepository).findByIdAndEnabledTrue(888L);
        verify(appointmentRepository, never()).findConflictingAppointments(anyLong(), any(), any());
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    @DisplayName("Should cancel appointment successfully when status is scheduled")
    void cancelAppointment_ScheduledStatus_SuccessfullyCancels() {
        // Arrange
        Appointment scheduledAppointment = Appointment.builder()
                .id(100L)
                .doctor(testDoctor)
                .patient(testPatient)
                .appointmentDateTime(futureDateTime)
                .notes("Regular checkup")
                .status(AppointmentStatus.SCHEDULED)
                .build();

        when(appointmentRepository.findById(100L)).thenReturn(Optional.of(scheduledAppointment));
        
        Appointment cancelledAppointment = Appointment.builder()
                .id(100L)
                .doctor(testDoctor)
                .patient(testPatient)
                .appointmentDateTime(futureDateTime)
                .notes("Regular checkup")
                .status(AppointmentStatus.CANCELLED)
                .cancellationReason("Patient requested")
                .build();

        when(appointmentRepository.save(any(Appointment.class))).thenReturn(cancelledAppointment);
        
        AppointmentResponse expectedResponse = AppointmentResponse.builder()
                .id(100L)
                .doctorId(1L)
                .doctorName("Dr. John Smith")
                .patientId(2L)
                .patientName("Jane Doe")
                .appointmentDateTime(futureDateTime)
                .notes("Regular checkup")
                .status(AppointmentStatus.CANCELLED)
                .cancellationReason("Patient requested")
                .build();
        
        when(appointmentMapper.toAppointmentResponse(cancelledAppointment)).thenReturn(expectedResponse);

        // Act
        AppointmentResponse actualResponse = appointmentService.cancelAppointment(100L, "Patient requested");

        // Assert
        assertNotNull(actualResponse);
        assertEquals(100L, actualResponse.getId());
        assertEquals(AppointmentStatus.CANCELLED, actualResponse.getStatus());
        assertEquals("Patient requested", actualResponse.getCancellationReason());

        verify(appointmentRepository).findById(100L);
        verify(appointmentRepository).save(any(Appointment.class));
        verify(appointmentMapper).toAppointmentResponse(cancelledAppointment);
    }

    @Test
    @DisplayName("Should throw InvalidAppointmentTimeException when trying to cancel already cancelled appointment")
    void cancelAppointment_AlreadyCancelled_ThrowsInvalidAppointmentTimeException() {
        // Arrange
        Appointment cancelledAppointment = Appointment.builder()
                .id(100L)
                .status(AppointmentStatus.CANCELLED)
                .build();

        when(appointmentRepository.findById(100L)).thenReturn(Optional.of(cancelledAppointment));

        // Act & Assert
        assertThrows(InvalidAppointmentTimeException.class, 
                () -> appointmentService.cancelAppointment(100L, "Test reason"));

        verify(appointmentRepository).findById(100L);
        verify(appointmentRepository, never()).save(any(Appointment.class));
        verify(appointmentMapper, never()).toAppointmentResponse(any());
    }

    @Test
    @DisplayName("Should throw InvalidAppointmentTimeException when trying to cancel completed appointment")
    void cancelAppointment_CompletedStatus_ThrowsInvalidAppointmentTimeException() {
        // Arrange
        Appointment completedAppointment = Appointment.builder()
                .id(100L)
                .status(AppointmentStatus.COMPLETED)
                .build();

        when(appointmentRepository.findById(100L)).thenReturn(Optional.of(completedAppointment));

        // Act & Assert
        assertThrows(InvalidAppointmentTimeException.class, 
                () -> appointmentService.cancelAppointment(100L, "Test reason"));

        verify(appointmentRepository).findById(100L);
        verify(appointmentRepository, never()).save(any(Appointment.class));
        verify(appointmentMapper, never()).toAppointmentResponse(any());
    }
}

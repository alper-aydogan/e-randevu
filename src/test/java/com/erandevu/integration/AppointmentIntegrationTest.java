package com.erandevu.integration;

import com.erandevu.ERandevuApplication;
import com.erandevu.dto.request.AppointmentRequest;
import com.erandevu.dto.response.AppointmentResponse;
import com.erandevu.entity.User;
import com.erandevu.enums.Role;
import com.erandevu.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for Appointment API.
 * Tests full flow: Controller -> UseCase -> Domain -> Repository -> Database
 */
@SpringBootTest(classes = ERandevuApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Appointment API Integration Tests")
class AppointmentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private User doctor;
    private User patient;

    @BeforeEach
    void setUp() {
        // Configure ObjectMapper for Java 8 date/time
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Create test users
        doctor = createTestUser("doctor1", "doctor1@test.com", Role.DOCTOR);
        patient = createTestUser("patient1", "patient1@test.com", Role.PATIENT);
    }

    private User createTestUser(String username, String email, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("password"));
        user.setRole(role);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setPhoneNumber("+905551234567");
        return userRepository.save(user);
    }

    @Test
    @WithMockUser(username = "patient1", roles = "PATIENT")
    @DisplayName("Should create appointment successfully")
    void shouldCreateAppointmentSuccessfully() throws Exception {
        // Given: Future appointment (next day at 10:00 AM)
        LocalDateTime appointmentTime = LocalDateTime.now()
            .plusDays(1)
            .withHour(10)
            .withMinute(0)
            .withSecond(0)
            .withNano(0);

        AppointmentRequest request = AppointmentRequest.builder()
            .doctorId(doctor.getId())
            .appointmentDateTime(appointmentTime)
            .notes("Regular checkup")
            .build();

        // When: POST request
        MvcResult result = mockMvc.perform(post("/api/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn();

        // Then: Verify response
        String responseBody = result.getResponse().getContentAsString();
        AppointmentResponse response = objectMapper.readValue(responseBody, AppointmentResponse.class);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotNull();
        assertThat(response.getDoctorId()).isEqualTo(doctor.getId());
        assertThat(response.getDoctorName()).isEqualTo(doctor.getFirstName() + " " + doctor.getLastName());
        assertThat(response.getPatientId()).isEqualTo(patient.getId());
        assertThat(response.getNotes()).isEqualTo("Regular checkup");
        assertThat(response.getStatus()).isEqualTo("SCHEDULED");
    }

    @Test
    @WithMockUser(username = "patient1", roles = "PATIENT")
    @DisplayName("Should reject appointment in the past")
    void shouldRejectAppointmentInThePast() throws Exception {
        // Given: Past appointment time
        LocalDateTime pastTime = LocalDateTime.now().minusDays(1);

        AppointmentRequest request = AppointmentRequest.builder()
            .doctorId(doctor.getId())
            .appointmentDateTime(pastTime)
            .notes("Past appointment")
            .build();

        // When & Then: Should get bad request
        mockMvc.perform(post("/api/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "patient1", roles = "PATIENT")
    @DisplayName("Should reject appointment on weekend")
    void shouldRejectAppointmentOnWeekend() throws Exception {
        // Given: Next Saturday at 10:00 AM
        LocalDateTime saturday = LocalDateTime.now()
            .plusDays(1 + (6 - LocalDateTime.now().getDayOfWeek().getValue()))
            .withHour(10)
            .withMinute(0);

        AppointmentRequest request = AppointmentRequest.builder()
            .doctorId(doctor.getId())
            .appointmentDateTime(saturday)
            .notes("Weekend appointment")
            .build();

        // When & Then: Should get bad request
        mockMvc.perform(post("/api/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "patient1", roles = "PATIENT")
    @DisplayName("Should reject appointment outside business hours")
    void shouldRejectAppointmentOutsideBusinessHours() throws Exception {
        // Given: Next day at 7:00 PM (after business hours)
        LocalDateTime eveningTime = LocalDateTime.now()
            .plusDays(1)
            .withHour(19)
            .withMinute(0);

        AppointmentRequest request = AppointmentRequest.builder()
            .doctorId(doctor.getId())
            .appointmentDateTime(eveningTime)
            .notes("Evening appointment")
            .build();

        // When & Then: Should get bad request
        mockMvc.perform(post("/api/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "patient1", roles = "PATIENT")
    @DisplayName("Should reject appointment less than 2 hours in advance")
    void shouldRejectAppointmentLessThanTwoHoursInAdvance() throws Exception {
        // Given: 1 hour from now
        LocalDateTime tooSoon = LocalDateTime.now().plusHours(1);

        AppointmentRequest request = AppointmentRequest.builder()
            .doctorId(doctor.getId())
            .appointmentDateTime(tooSoon)
            .notes("Urgent appointment")
            .build();

        // When & Then: Should get bad request
        mockMvc.perform(post("/api/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
}

package com.erandevu.test.controller;
import com.erandevu.dto.AppointmentRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest
@AutoConfigureMockMvc
public class AppointmentControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    private AppointmentRequestDTO appointmentRequest;
    @BeforeEach
    public void setUp() {
        appointmentRequest = new AppointmentRequestDTO();
        appointmentRequest.setDoctorId(1L);
        appointmentRequest.setAppointmentDateTime("2024-12-25T10:30:00");
        appointmentRequest.setNotes("Regular checkup");
    }
    @Test
    @WithMockUser(roles = "PATIENT")
    public void testCreateAppointment_Success() throws Exception {
        mockMvc.perform(post("/api/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(appointmentRequest)))
                .andExpect(status().isCreated());
    }
    @Test
    @WithMockUser(roles = "PATIENT")
    public void testCreateAppointment_InvalidTime() throws Exception {
        appointmentRequest.setAppointmentDateTime("2024-01-01T08:00:00");
        mockMvc.perform(post("/api/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(appointmentRequest)))
                .andExpect(status().isBadRequest());
    }
    @Test
    @WithMockUser(roles = "PATIENT")
    public void testGetAppointmentById_Success() throws Exception {
        mockMvc.perform(get("/api/appointments/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
    @Test
    @WithMockUser(roles = "DOCTOR")
    public void testGetDoctorAppointments() throws Exception {
        mockMvc.perform(get("/api/appointments/doctor/1?page=0&size=10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
    @Test
    @WithMockUser(roles = "PATIENT")
    public void testGetPatientAppointments() throws Exception {
        mockMvc.perform(get("/api/appointments/patient/1?page=0&size=10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
    @Test
    @WithMockUser(roles = "PATIENT")
    public void testCancelAppointment_Success() throws Exception {
        mockMvc.perform(put("/api/appointments/1/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cancellationReason\":\"Patient requested\"}")).
                andExpect(status().isOk());
    }
    @Test
    public void testCreateAppointment_Unauthorized() throws Exception {
        mockMvc.perform(post("/api/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(appointmentRequest)))
                .andExpect(status().isUnauthorized());
    }
}
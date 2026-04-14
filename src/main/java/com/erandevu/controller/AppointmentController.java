package com.erandevu.controller;

import com.erandevu.application.appointment.CreateAppointmentCommand;
import com.erandevu.application.appointment.CreateAppointmentUseCase;
import com.erandevu.dto.request.AppointmentRequest;
import com.erandevu.dto.response.AppointmentResponse;
import com.erandevu.dto.response.PageResponse;
import com.erandevu.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Appointment management controller.
 * Clean Architecture: Controller only calls Application Use Cases, never Services directly.
 * SECURITY: Patient ID is always extracted from JWT token - never from request body.
 */
@RestController
@RequestMapping("/api/appointments")
@Tag(name = "Appointments", description = "Appointment management endpoints")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class AppointmentController {

    private final CreateAppointmentUseCase createAppointmentUseCase;

    @PostMapping
    @Operation(summary = "Create appointment", description = "Creates a new appointment for the authenticated patient")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<AppointmentResponse> createAppointment(
            @Valid @RequestBody AppointmentRequest request,
            @AuthenticationPrincipal User user) {
        
        // SECURITY: Patient ID extracted from JWT token, not from request body
        Long patientId = user.getId();
        
        // Create command and execute use case (Clean Architecture)
        CreateAppointmentCommand command = CreateAppointmentCommand.of(
            request.getDoctorId(),
            patientId,
            request.getAppointmentDateTime(),
            request.getNotes()
        );
        
        AppointmentResponse response = createAppointmentUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

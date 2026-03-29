package com.erandevu.controller;

import com.erandevu.dto.request.AppointmentRequest;
import com.erandevu.dto.response.AppointmentResponse;
import com.erandevu.service.ConcurrentAppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/appointments")
@Tag(name = "Concurrent Appointments", description = "Concurrent-safe appointment management endpoints")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Slf4j
public class ConcurrentAppointmentController {

    private final ConcurrentAppointmentService appointmentService;

    @PostMapping
    @Operation(summary = "Create appointment with concurrency control", 
             description = "Creates a new appointment with comprehensive concurrency and consistency guarantees")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'PATIENT')")
    public ResponseEntity<AppointmentResponse> createAppointment(@Valid @RequestBody AppointmentRequest request) {
        log.info("Received appointment creation request: doctorId={}, patientId={}", 
                request.getDoctorId(), request.getPatientId());
        
        try {
            AppointmentResponse response = appointmentService.createAppointment(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            log.error("Error creating appointment", e);
            throw e; // Let global exception handler handle it
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update appointment with optimistic locking", 
             description = "Updates an existing appointment with optimistic concurrency control")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'PATIENT')")
    public ResponseEntity<AppointmentResponse> updateAppointment(
            @Parameter(description = "Appointment ID") @PathVariable Long id,
            @Valid @RequestBody AppointmentRequest request) {
        
        log.info("Received appointment update request: id={}", id);
        
        try {
            AppointmentResponse response = appointmentService.updateAppointment(id, request);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error updating appointment: id={}", id, e);
            throw e; // Let global exception handler handle it
        }
    }

    @DeleteMapping("/{id}/cancel")
    @Operation(summary = "Cancel appointment with pessimistic locking", 
             description = "Cancels an appointment using pessimistic locking for consistency")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'PATIENT')")
    public ResponseEntity<Void> cancelAppointment(
            @Parameter(description = "Appointment ID") @PathVariable Long id) {
        
        log.info("Received appointment cancellation request: id={}", id);
        
        try {
            appointmentService.cancelAppointment(id);
            return ResponseEntity.noContent().build();
            
        } catch (Exception e) {
            log.error("Error cancelling appointment: id={}", id, e);
            throw e; // Let global exception handler handle it
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get appointment by ID", 
             description = "Returns appointment information (read-only operation)")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'PATIENT')")
    public ResponseEntity<AppointmentResponse> getAppointmentById(
            @Parameter(description = "Appointment ID") @PathVariable Long id) {
        
        log.debug("Getting appointment by ID: {}", id);
        
        try {
            AppointmentResponse response = appointmentService.getAppointmentById(id);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting appointment: id={}", id, e);
            throw e; // Let global exception handler handle it
        }
    }

    /**
     * Demonstrates async appointment creation (for high-throughput scenarios)
     * In production, this would be used with proper async configuration
     */
    @PostMapping("/async")
    @Operation(summary = "Create appointment asynchronously", 
             description = "Creates an appointment asynchronously for high-throughput scenarios")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'PATIENT')")
    public CompletableFuture<ResponseEntity<AppointmentResponse>> createAppointmentAsync(@Valid @RequestBody AppointmentRequest request) {
        log.info("Received async appointment creation request: doctorId={}, patientId={}", 
                request.getDoctorId(), request.getPatientId());
        
        return CompletableFuture
                .supplyAsync(() -> appointmentService.createAppointment(request))
                .orTimeout(5, TimeUnit.SECONDS)
                .thenApply(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
                .exceptionally(throwable -> {
                    log.error("Error in async appointment creation", throwable);
                    throw new RuntimeException("Appointment creation failed", throwable);
                });
    }

    /**
     * Health check endpoint for concurrency monitoring
     */
    @GetMapping("/health")
    @Operation(summary = "Concurrency health check", 
             description = "Returns the health status of the concurrency system")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ConcurrencyHealthStatus> getConcurrencyHealth() {
        try {
            // In a real system, this would check various metrics
            var lockStats = com.erandevu.util.ConcurrentAppointmentUtils.getLockStatistics();
            
            ConcurrencyHealthStatus status = ConcurrencyHealthStatus.builder()
                    .healthy(true)
                    .activeLocks(lockStats.getTotalDoctorLocks())
                    .globalLockLocked(lockStats.getGlobalLockLocked())
                    .timestamp(java.time.LocalDateTime.now())
                    .build();
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            log.error("Error checking concurrency health", e);
            
            ConcurrencyHealthStatus status = ConcurrencyHealthStatus.builder()
                    .healthy(false)
                    .error(e.getMessage())
                    .timestamp(java.time.LocalDateTime.now())
                    .build();
            
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(status);
        }
    }

    /**
     * Concurrency health status DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class ConcurrencyHealthStatus {
        private Boolean healthy;
        private Integer activeLocks;
        private Boolean globalLockLocked;
        private String error;
        private java.time.LocalDateTime timestamp;
    }
}

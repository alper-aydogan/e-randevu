package com.erandevu.security;

import com.erandevu.entity.Appointment;
import com.erandevu.entity.User;
import com.erandevu.enums.Role;
import com.erandevu.repository.AppointmentRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Centralizes application-level authorization checks used by method security.
 */
@Component("authz")
public class AuthorizationService {

    private final AppointmentRepository appointmentRepository;

    public AuthorizationService(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    public boolean canAccessUserById(Authentication authentication, Long userId) {
        User currentUser = extractUser(authentication);
        return currentUser != null && (isAdmin(currentUser) || currentUser.getId().equals(userId));
    }

    public boolean canAccessUserByUsername(Authentication authentication, String username) {
        User currentUser = extractUser(authentication);
        return currentUser != null && (isAdmin(currentUser) || currentUser.getUsername().equals(username));
    }

    public boolean canListDoctors(Authentication authentication) {
        return extractUser(authentication) != null;
    }

    public boolean canListPatients(Authentication authentication) {
        User currentUser = extractUser(authentication);
        return currentUser != null && (isAdmin(currentUser) || currentUser.getRole() == Role.DOCTOR);
    }

    public boolean canAccessAppointment(Authentication authentication, Long appointmentId) {
        User currentUser = extractUser(authentication);
        if (currentUser == null) {
            return false;
        }

        if (isAdmin(currentUser)) {
            return true;
        }

        return appointmentRepository.findById(appointmentId)
                .map(appointment -> isParticipant(currentUser, appointment))
                .orElse(false);
    }

    public boolean canAccessDoctorAppointments(Authentication authentication, Long doctorId) {
        User currentUser = extractUser(authentication);
        return currentUser != null && (isAdmin(currentUser)
                || (currentUser.getRole() == Role.DOCTOR && currentUser.getId().equals(doctorId)));
    }

    public boolean canAccessPatientAppointments(Authentication authentication, Long patientId) {
        User currentUser = extractUser(authentication);
        return currentUser != null && (isAdmin(currentUser)
                || (currentUser.getRole() == Role.PATIENT && currentUser.getId().equals(patientId)));
    }

    private boolean isAdmin(User user) {
        return user.getRole() == Role.ADMIN;
    }

    private boolean isParticipant(User user, Appointment appointment) {
        return appointment.getDoctor().getId().equals(user.getId())
                || appointment.getPatient().getId().equals(user.getId());
    }

    private User extractUser(Authentication authentication) {
        if (authentication == null) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user;
        }

        return null;
    }
}

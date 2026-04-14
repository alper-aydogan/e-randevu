package com.erandevu.security;

import com.erandevu.entity.User;
import com.erandevu.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationServiceTest {

    private AuthorizationService authorizationService;

    @BeforeEach
    void setUp() {
        authorizationService = new AuthorizationService();
    }

    @Test
    void canAccessUserById_AllowsAdminForAnyUser() {
        Authentication authentication = authenticationFor(user(1L, "admin", Role.ADMIN));

        assertThat(authorizationService.canAccessUserById(authentication, 99L)).isTrue();
    }

    @Test
    void canAccessUserById_AllowsUserForOwnRecord() {
        Authentication authentication = authenticationFor(user(7L, "patient_7", Role.PATIENT));

        assertThat(authorizationService.canAccessUserById(authentication, 7L)).isTrue();
    }

    @Test
    void canAccessUserById_DeniesUserForAnotherRecord() {
        Authentication authentication = authenticationFor(user(7L, "patient_7", Role.PATIENT));

        assertThat(authorizationService.canAccessUserById(authentication, 8L)).isFalse();
    }

    @Test
    void canAccessUserByUsername_AllowsOwnUsername() {
        Authentication authentication = authenticationFor(user(2L, "doctor_2", Role.DOCTOR));

        assertThat(authorizationService.canAccessUserByUsername(authentication, "doctor_2")).isTrue();
    }

    @Test
    void canAccessUserByUsername_DeniesOtherUsername() {
        Authentication authentication = authenticationFor(user(2L, "doctor_2", Role.DOCTOR));

        assertThat(authorizationService.canAccessUserByUsername(authentication, "patient_1")).isFalse();
    }

    @Test
    void canListPatients_AllowsDoctorAndAdminOnly() {
        Authentication doctor = authenticationFor(user(10L, "doctor", Role.DOCTOR));
        Authentication admin = authenticationFor(user(1L, "admin", Role.ADMIN));
        Authentication patient = authenticationFor(user(20L, "patient", Role.PATIENT));

        assertThat(authorizationService.canListPatients(doctor)).isTrue();
        assertThat(authorizationService.canListPatients(admin)).isTrue();
        assertThat(authorizationService.canListPatients(patient)).isFalse();
    }

    private Authentication authenticationFor(User user) {
        return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    }

    private User user(Long id, String username, Role role) {
        return User.builder()
                .id(id)
                .username(username)
                .password("Password123")
                .email(username + "@example.com")
                .firstName("Test")
                .lastName("User")
                .phoneNumber("+905551112233")
                .role(role)
                .enabled(true)
                .build();
    }
}

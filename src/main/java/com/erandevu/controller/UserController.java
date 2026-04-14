package com.erandevu.controller;

import com.erandevu.dto.response.PageResponse;
import com.erandevu.dto.response.UserResponse;
import com.erandevu.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "User management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Returns user information by ID")
    @PreAuthorize("@authz.canAccessUserById(authentication, #id)")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/username/{username}")
    @Operation(summary = "Get user by username", description = "Returns user information by username")
    @PreAuthorize("@authz.canAccessUserByUsername(authentication, #username)")
    public ResponseEntity<UserResponse> getUserByUsername(@PathVariable String username) {
        UserResponse response = userService.getUserByUsername(username);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get all users", description = "Returns all active users with pagination")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<UserResponse>> getAllUsers(
            @Parameter(description = "Page number (0-based)", example = "0") 
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "10") 
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort by field", example = "id") 
            @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction", example = "asc") 
            @RequestParam(defaultValue = "asc") String sortDir) {
        PageResponse<UserResponse> response = userService.getAllUsers(page, size, sortBy, sortDir);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/doctors")
    @Operation(summary = "Get all doctors", description = "Returns all active doctors with pagination")
    @PreAuthorize("@authz.canListDoctors(authentication)")
    public ResponseEntity<PageResponse<UserResponse>> getAllDoctors(
            @Parameter(description = "Page number (0-based)", example = "0") 
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "10") 
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort by field", example = "firstName") 
            @RequestParam(defaultValue = "firstName") String sortBy,
            @Parameter(description = "Sort direction", example = "asc") 
            @RequestParam(defaultValue = "asc") String sortDir) {
        PageResponse<UserResponse> response = userService.getAllDoctorsPaginated(page, size, sortBy, sortDir);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/patients")
    @Operation(summary = "Get all patients", description = "Returns all active patients with pagination")
    @PreAuthorize("@authz.canListPatients(authentication)")
    public ResponseEntity<PageResponse<UserResponse>> getAllPatients(
            @Parameter(description = "Page number (0-based)", example = "0") 
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "10") 
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort by field", example = "firstName") 
            @RequestParam(defaultValue = "firstName") String sortBy,
            @Parameter(description = "Sort direction", example = "asc") 
            @RequestParam(defaultValue = "asc") String sortDir) {
        PageResponse<UserResponse> response = userService.getAllPatientsPaginated(page, size, sortBy, sortDir);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/toggle-status")
    @Operation(summary = "Toggle user status", description = "Enables or disables a user account")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> toggleUserStatus(@PathVariable Long id) {
        UserResponse response = userService.toggleUserStatus(id);
        return ResponseEntity.ok(response);
    }
}

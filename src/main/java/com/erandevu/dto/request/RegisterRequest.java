package com.erandevu.dto.request;

import com.erandevu.dto.validation.ValidationMessages;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User registration request DTO.
 * SECURITY: Role is NOT included - assigned server-side as PATIENT by default.
 * Prevents privilege escalation attacks.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User registration request. Role is automatically assigned as PATIENT.")
public class RegisterRequest {

    @NotBlank(message = ValidationMessages.USERNAME_REQUIRED)
    @Size(min = 3, max = 50, message = ValidationMessages.USERNAME_SIZE)
    @Schema(description = "Unique username for login", example = "johndoe", required = true)
    private String username;

    @NotBlank(message = ValidationMessages.PASSWORD_REQUIRED)
    @Size(min = 6, max = 100, message = ValidationMessages.PASSWORD_SIZE)
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
        message = ValidationMessages.PASSWORD_PATTERN
    )
    @Schema(description = "Password with at least 1 uppercase, 1 lowercase, 1 digit",
            example = "Password123",
            required = true)
    private String password;

    @NotBlank(message = ValidationMessages.EMAIL_REQUIRED)
    @Email(message = ValidationMessages.EMAIL_INVALID)
    @Schema(description = "Valid email address", example = "john.doe@example.com", required = true)
    private String email;

    @NotBlank(message = ValidationMessages.FIRST_NAME_REQUIRED)
    @Size(max = 50, message = ValidationMessages.FIRST_NAME_SIZE)
    @Schema(description = "User's first name", example = "John", required = true)
    private String firstName;

    @NotBlank(message = ValidationMessages.LAST_NAME_REQUIRED)
    @Size(max = 50, message = ValidationMessages.LAST_NAME_SIZE)
    @Schema(description = "User's last name", example = "Doe", required = true)
    private String lastName;

    @NotBlank(message = ValidationMessages.PHONE_REQUIRED)
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = ValidationMessages.PHONE_PATTERN)
    @Schema(description = "Phone number with optional country code",
            example = "+1234567890",
            required = true)
    private String phoneNumber;

    // ❌ REMOVED: private Role role - SECURITY RISK! Privilege escalation possible
}

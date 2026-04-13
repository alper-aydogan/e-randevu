package com.erandevu.dto.validation;

/**
 * Centralized validation messages for DTO layer.
 * Ensures consistency across all API validation errors.
 */
public final class ValidationMessages {

    private ValidationMessages() {
        // Utility class
    }

    // Common
    public static final String REQUIRED = "is required";
    public static final String REQUIRED_FIELD = "{field} is required";
    public static final String INVALID_FORMAT = "Invalid format";
    public static final String SIZE_BETWEEN = "Must be between {min} and {max} characters";
    public static final String MAX_SIZE = "Must not exceed {max} characters";

    // User
    public static final String USERNAME_REQUIRED = "Username is required";
    public static final String USERNAME_SIZE = "Username must be between 3 and 50 characters";
    public static final String PASSWORD_REQUIRED = "Password is required";
    public static final String PASSWORD_SIZE = "Password must be between 6 and 100 characters";
    public static final String PASSWORD_PATTERN = "Password must contain at least one uppercase letter, one lowercase letter, and one digit";
    public static final String EMAIL_REQUIRED = "Email is required";
    public static final String EMAIL_INVALID = "Email should be valid";
    public static final String FIRST_NAME_REQUIRED = "First name is required";
    public static final String FIRST_NAME_SIZE = "First name must not exceed 50 characters";
    public static final String LAST_NAME_REQUIRED = "Last name is required";
    public static final String LAST_NAME_SIZE = "Last name must not exceed 50 characters";
    public static final String PHONE_REQUIRED = "Phone number is required";
    public static final String PHONE_PATTERN = "Phone number should be valid (10-15 digits, optional + prefix)";
    public static final String ROLE_REQUIRED = "Role is required";

    // Appointment
    public static final String DOCTOR_ID_REQUIRED = "Doctor ID is required";
    public static final String DOCTOR_ID_POSITIVE = "Doctor ID must be positive";
    public static final String PATIENT_ID_REQUIRED = "Patient ID is required";
    public static final String PATIENT_ID_POSITIVE = "Patient ID must be positive";
    public static final String APPOINTMENT_DATETIME_REQUIRED = "Appointment date and time is required";
    public static final String APPOINTMENT_DATETIME_FUTURE = "Appointment date and time must be in the future";
    public static final String NOTES_SIZE = "Notes must not exceed 500 characters";
}

package com.erandevu.util;

import com.erandevu.exception.InvalidAppointmentTimeException;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.regex.Pattern;

/**
 * Utility class for common validation operations
 */
@Slf4j
public class ValidationUtils {

    // Phone number pattern: + followed by 10-15 digits
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9]{10,15}$");
    
    // Email pattern (basic validation)
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    /**
     * Validates phone number format
     */
    public static boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }
        return PHONE_PATTERN.matcher(phoneNumber).matches();
    }

    /**
     * Validates email format
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Validates password strength
     * - At least 8 characters
     * - At least one uppercase letter
     * - At least one lowercase letter
     * - At least one digit
     */
    public static boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
        }
        
        return hasUpper && hasLower && hasDigit;
    }

    /**
     * Validates username format
     * - 3-50 characters
     * - Only letters, numbers, and underscores
     */
    public static boolean isValidUsername(String username) {
        if (username == null || username.length() < 3 || username.length() > 50) {
            return false;
        }
        return username.matches("^[a-zA-Z0-9_]+$");
    }

    /**
     * Checks if a given time is within business hours (9 AM - 6 PM)
     */
    public static boolean isWithinBusinessHours(LocalTime time) {
        return !time.isBefore(LocalTime.of(9, 0)) && !time.isAfter(LocalTime.of(18, 0));
    }

    /**
     * Checks if a given datetime is within business hours
     */
    public static boolean isWithinBusinessHours(LocalDateTime dateTime) {
        return isWithinBusinessHours(dateTime.toLocalTime());
    }

    /**
     * Checks if a given date is a weekday (Monday-Friday)
     */
    public static boolean isWeekday(LocalDateTime dateTime) {
        java.time.DayOfWeek dayOfWeek = dateTime.getDayOfWeek();
        return dayOfWeek != java.time.DayOfWeek.SATURDAY && 
               dayOfWeek != java.time.DayOfWeek.SUNDAY;
    }

    /**
     * Validates appointment booking time constraints
     */
    public static void validateAppointmentBookingTime(LocalDateTime appointmentDateTime) {
        LocalDateTime now = LocalDateTime.now();
        
        // Check if appointment is at least 2 hours in the future
        if (appointmentDateTime.isBefore(now.plusHours(2))) {
            throw new InvalidAppointmentTimeException(
                    "Appointments must be booked at least 2 hours in advance", 
                    appointmentDateTime, 
                    "TOO_SOON");
        }
        
        // Check if appointment is not more than 30 days in the future
        if (appointmentDateTime.isAfter(now.plusDays(30))) {
            throw new InvalidAppointmentTimeException(
                    "Appointments cannot be booked more than 30 days in advance", 
                    appointmentDateTime, 
                    "TOO_FAR");
        }
        
        // Check if appointment is on a weekday
        if (!isWeekday(appointmentDateTime)) {
            throw new InvalidAppointmentTimeException(
                    "Appointments are only available on weekdays", 
                    appointmentDateTime, 
                    "WEEKEND");
        }
        
        // Check if appointment is during business hours
        if (!isWithinBusinessHours(appointmentDateTime)) {
            throw new InvalidAppointmentTimeException(
                    "Appointments are only available between 9:00 AM and 6:00 PM", 
                    appointmentDateTime, 
                    "OUTSIDE_HOURS");
        }
    }

    /**
     * Validates text field length
     */
    public static boolean isValidTextLength(String text, int minLength, int maxLength) {
        if (text == null) {
            return minLength == 0;
        }
        int length = text.trim().length();
        return length >= minLength && length <= maxLength;
    }

    /**
     * Sanitizes text input by trimming and removing excessive whitespace
     */
    public static String sanitizeText(String text) {
        if (text == null) {
            return null;
        }
        return text.trim().replaceAll("\\s+", " ");
    }

    /**
     * Validates that two IDs are not the same
     */
    public static void validateDifferentIds(Long id1, Long id2, String errorMessage) {
        if (id1 != null && id1.equals(id2)) {
            throw new InvalidAppointmentTimeException(
                    errorMessage, 
                    null, 
                    "SAME_ID");
        }
    }

    /**
     * Checks if a string is null or empty after trimming
     */
    public static boolean isNullOrEmpty(String text) {
        return text == null || text.trim().isEmpty();
    }

    /**
     * Validates that a number is positive
     */
    public static boolean isPositive(Number number) {
        return number != null && number.doubleValue() > 0;
    }
}

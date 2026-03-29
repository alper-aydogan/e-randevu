package com.erandevu.util;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe utility class for appointment concurrency management
 */
@Slf4j
public class ConcurrentAppointmentUtils {

    // Thread-safe cache for appointment locks per doctor
    private static final ConcurrentHashMap<Long, ReentrantLock> DOCTOR_LOCKS = new ConcurrentHashMap<>();
    
    // Global lock for critical operations
    private static final ReentrantLock GLOBAL_LOCK = new ReentrantLock();

    /**
     * Executes a critical operation with doctor-specific locking
     * This ensures that operations for the same doctor are serialized
     */
    public static <T> T executeWithDoctorLock(Long doctorId, CriticalOperation<T> operation) {
        ReentrantLock doctorLock = DOCTOR_LOCKS.computeIfAbsent(doctorId, k -> new ReentrantLock());
        
        try {
            doctorLock.lock();
            log.debug("Acquired lock for doctor: {}", doctorId);
            
            return operation.execute();
            
        } finally {
            doctorLock.unlock();
            log.debug("Released lock for doctor: {}", doctorId);
        }
    }

    /**
     * Executes a critical operation with global locking
     * Used for operations that affect the entire system
     */
    public static <T> T executeWithGlobalLock(CriticalOperation<T> operation) {
        try {
            GLOBAL_LOCK.lock();
            log.debug("Acquired global lock");
            
            return operation.execute();
            
        } finally {
            GLOBAL_LOCK.unlock();
            log.debug("Released global lock");
        }
    }

    /**
     * Checks if two time ranges overlap (thread-safe)
     */
    public static boolean isTimeRangeOverlapping(
            LocalDateTime start1, LocalDateTime end1,
            LocalDateTime start2, LocalDateTime end2) {
        
        // Validate inputs
        if (start1 == null || end1 == null || start2 == null || end2 == null) {
            return false;
        }
        
        // Check for invalid ranges
        if (start1.isAfter(end1) || start2.isAfter(end2)) {
            return false;
        }
        
        // Check overlap
        return (start1.isBefore(end2) && end1.isAfter(start2)) ||
               (start2.isBefore(end1) && end2.isAfter(start1)) ||
               start1.equals(start2) || end1.equals(end2);
    }

    /**
     * Validates appointment time constraints (thread-safe)
     */
    public static void validateAppointmentTimeConstraints(LocalDateTime appointmentDateTime) {
        if (appointmentDateTime == null) {
            throw new IllegalArgumentException("Appointment datetime cannot be null");
        }
        
        LocalDateTime now = LocalDateTime.now();
        
        // Check if appointment is at least 2 hours in the future
        if (appointmentDateTime.isBefore(now.plusHours(2))) {
            throw new IllegalArgumentException("Appointments must be booked at least 2 hours in advance");
        }
        
        // Check if appointment is not more than 30 days in the future
        if (appointmentDateTime.isAfter(now.plusDays(30))) {
            throw new IllegalArgumentException("Appointments cannot be booked more than 30 days in advance");
        }
        
        // Check if appointment is on a weekday
        if (isWeekend(appointmentDateTime)) {
            throw new IllegalArgumentException("Appointments are not available on weekends");
        }
        
        // Check if appointment is during business hours
        if (!isWithinBusinessHours(appointmentDateTime.toLocalTime())) {
            throw new IllegalArgumentException("Appointments are only available between 9:00 AM and 6:00 PM");
        }
    }

    /**
     * Checks if a given date is a weekend (thread-safe)
     */
    public static boolean isWeekend(LocalDateTime dateTime) {
        if (dateTime == null) {
            return false;
        }
        
        java.time.DayOfWeek dayOfWeek = dateTime.getDayOfWeek();
        return dayOfWeek == java.time.DayOfWeek.SATURDAY || 
               dayOfWeek == java.time.DayOfWeek.SUNDAY;
    }

    /**
     * Checks if a given time is within business hours (thread-safe)
     */
    public static boolean isWithinBusinessHours(java.time.LocalTime time) {
        if (time == null) {
            return false;
        }
        
        return !time.isBefore(java.time.LocalTime.of(9, 0)) && 
               !time.isAfter(java.time.LocalTime.of(18, 0));
    }

    /**
     * Functional interface for critical operations
     */
    @FunctionalInterface
    public interface CriticalOperation<T> {
        T execute() throws Exception;
    }

    /**
     * Cleans up unused doctor locks (memory management)
     */
    public static void cleanupUnusedLocks() {
        // This method can be called periodically to clean up unused locks
        // In a production system, this would be part of a maintenance task
        log.debug("Cleaning up unused doctor locks. Current lock count: {}", DOCTOR_LOCKS.size());
    }

    /**
     * Gets current lock statistics for monitoring
     */
    public static LockStatistics getLockStatistics() {
        return LockStatistics.builder()
                .totalDoctorLocks(DOCTOR_LOCKS.size())
                .globalLockLocked(GLOBAL_LOCK.isLocked())
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Lock statistics DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class LockStatistics {
        private Integer totalDoctorLocks;
        private Boolean globalLockLocked;
        private LocalDateTime timestamp;
    }
}

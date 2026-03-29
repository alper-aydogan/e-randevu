package com.erandevu.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheManagementService {

    private final CacheManager cacheManager;

    /**
     * Clears all application caches
     */
    @CacheEvict(value = {
            "users",
            "doctors", 
            "patients",
            "appointments",
            "doctorAppointments",
            "patientAppointments",
            "appointmentsByStatus",
            "todayAppointments"
    }, allEntries = true)
    public void clearAllCaches() {
        log.info("Clearing all application caches");
    }

    /**
     * Clears user-related caches
     */
    @CacheEvict(value = {
            "users",
            "doctors", 
            "patients"
    }, allEntries = true)
    public void clearUserCaches() {
        log.info("Clearing user-related caches");
    }

    /**
     * Clears appointment-related caches
     */
    @CacheEvict(value = {
            "appointments",
            "doctorAppointments",
            "patientAppointments",
            "appointmentsByStatus",
            "todayAppointments"
    }, allEntries = true)
    public void clearAppointmentCaches() {
        log.info("Clearing appointment-related caches");
    }

    /**
     * Clears specific user caches
     */
    @CacheEvict(value = {
            "users",
            "doctors", 
            "patients"
    }, allEntries = true)
    public void clearUserCache(Long userId) {
        log.info("Clearing caches for user: {}", userId);
    }

    /**
     * Clears specific doctor caches
     */
    @CacheEvict(value = {
            "doctors",
            "doctorAppointments",
            "todayAppointments"
    }, allEntries = true)
    public void clearDoctorCache(Long doctorId) {
        log.info("Clearing caches for doctor: {}", doctorId);
    }

    /**
     * Gets cache statistics and information
     */
    public CacheStatistics getCacheStatistics() {
        Collection<String> cacheNames = cacheManager.getCacheNames();
        
        return CacheStatistics.builder()
                .totalCaches(cacheNames.size())
                .cacheNames(cacheNames)
                .timestamp(java.time.LocalDateTime.now())
                .build();
    }

    /**
     * Cache statistics DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class CacheStatistics {
        private Integer totalCaches;
        private Collection<String> cacheNames;
        private java.time.LocalDateTime timestamp;
    }
}

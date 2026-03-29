package com.erandevu.service;

import com.erandevu.dto.response.PageResponse;
import com.erandevu.dto.response.UserResponse;
import com.erandevu.entity.User;
import com.erandevu.enums.Role;
import com.erandevu.exception.ResourceNotFoundException;
import com.erandevu.mapper.UserMapper;
import com.erandevu.repository.UserRepository;
import com.erandevu.util.PageResponseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CachedUserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    /**
     * Gets user by ID with caching
     */
    @Cacheable(value = "users", key = "#id")
    public UserResponse getUserById(Long id) {
        log.debug("Getting user by ID: {}", id);
        User user = userRepository.findByIdAndEnabledTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return userMapper.toUserResponse(user);
    }

    /**
     * Gets user by username with caching
     */
    @Cacheable(value = "users", key = "#username")
    public UserResponse getUserByUsername(String username) {
        log.debug("Getting user by username: {}", username);
        User user = userRepository.findByUsernameAndEnabledTrue(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        return userMapper.toUserResponse(user);
    }

    /**
     * Gets paginated users by role with caching
     */
    @Cacheable(value = "users", key = "#role.name() + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public PageResponse<UserResponse> getUsersByRolePaginated(Role role, Pageable pageable) {
        log.debug("Getting users by role: {}, page: {}", role, pageable.getPageNumber());
        Page<User> users = userRepository.findByRoleAndEnabledTrue(role, pageable);
        return PageResponseUtil.create(users.map(userMapper::toUserResponse));
    }

    /**
     * Gets all users with pagination and caching
     */
    @Cacheable(value = "users", key = "'all_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public PageResponse<UserResponse> getAllUsersPaginated(Pageable pageable) {
        log.debug("Getting all users, page: {}", pageable.getPageNumber());
        Page<User> users = userRepository.findByEnabledTrue(pageable);
        return PageResponseUtil.create(users.map(userMapper::toUserResponse));
    }

    /**
     * Gets doctors with caching
     */
    @Cacheable(value = "doctors", key = "'all_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public PageResponse<UserResponse> getDoctorsPaginated(Pageable pageable) {
        log.debug("Getting doctors, page: {}", pageable.getPageNumber());
        Page<User> doctors = userRepository.findByRoleAndEnabledTrue(Role.DOCTOR, pageable);
        return PageResponseUtil.create(doctors.map(userMapper::toUserResponse));
    }

    /**
     * Gets patients with caching
     */
    @Cacheable(value = "users", key = "'patients_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public PageResponse<UserResponse> getPatientsPaginated(Pageable pageable) {
        log.debug("Getting patients, page: {}", pageable.getPageNumber());
        Page<User> patients = userRepository.findByRoleAndEnabledTrue(Role.PATIENT, pageable);
        return PageResponseUtil.create(patients.map(userMapper::toUserResponse));
    }

    /**
     * Searches users by name or email with caching
     */
    @Cacheable(value = "users", key = "'search_' + #query + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public PageResponse<UserResponse> searchUsers(String query, Pageable pageable) {
        log.debug("Searching users with query: {}", query);
        Page<User> users = userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCaseAndEnabledTrue(query, query, pageable);
        return PageResponseUtil.create(users.map(userMapper::toUserResponse));
    }

    /**
     * Creates new user and evicts relevant caches
     */
    @CacheEvict(value = {
            "users", 
            "doctors", 
            "patients"
    }, allEntries = true)
    public UserResponse createUser(User user) {
        log.info("Creating new user: {}", user.getUsername());
        User savedUser = userRepository.save(user);
        log.info("User created successfully with ID: {}", savedUser.getId());
        return userMapper.toUserResponse(savedUser);
    }

    /**
     * Updates user and evicts relevant caches
     */
    @CacheEvict(value = {
            "users", 
            "doctors", 
            "patients"
    }, allEntries = true)
    public UserResponse updateUser(Long id, User updatedUser) {
        log.info("Updating user: {}", id);
        User existingUser = userRepository.findByIdAndEnabledTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        
        // Update fields
        existingUser.setFirstName(updatedUser.getFirstName());
        existingUser.setLastName(updatedUser.getLastName());
        existingUser.setEmail(updatedUser.getEmail());
        existingUser.setPhoneNumber(updatedUser.getPhoneNumber());
        existingUser.setRole(updatedUser.getRole());
        
        User savedUser = userRepository.save(existingUser);
        log.info("User updated successfully: {}", savedUser.getId());
        return userMapper.toUserResponse(savedUser);
    }

    /**
     * Deletes user (soft delete) and evicts relevant caches
     */
    @CacheEvict(value = {
            "users", 
            "doctors", 
            "patients"
    }, allEntries = true)
    public void deleteUser(Long id) {
        log.info("Deleting user: {}", id);
        User user = userRepository.findByIdAndEnabledTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        
        user.setEnabled(false);
        userRepository.save(user);
        log.info("User deleted successfully: {}", id);
    }

    /**
     * Gets user statistics with caching
     */
    @Cacheable(value = "users", key = "'stats_' + #startDate.toString() + '_' + #endDate.toString()")
    public UserStatistics getUserStatistics(java.time.LocalDateTime startDate, java.time.LocalDateTime endDate) {
        log.debug("Getting user statistics from {} to {}", startDate, endDate);
        
        long totalUsers = userRepository.countByEnabledTrue();
        long doctors = userRepository.countByRoleAndEnabledTrue(Role.DOCTOR);
        long patients = userRepository.countByRoleAndEnabledTrue(Role.PATIENT);
        long admins = userRepository.countByRoleAndEnabledTrue(Role.ADMIN);

        return UserStatistics.builder()
                .totalUsers(totalUsers)
                .doctors(doctors)
                .patients(patients)
                .admins(admins)
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }

    /**
     * User statistics DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class UserStatistics {
        private Long totalUsers;
        private Long doctors;
        private Long patients;
        private Long admins;
        private java.time.LocalDateTime startDate;
        private java.time.LocalDateTime endDate;
    }
}

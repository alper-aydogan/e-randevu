package com.erandevu.repository;

import com.erandevu.entity.User;
import com.erandevu.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Optional<User> findByUsernameAndEnabledTrue(String username);
    Optional<User> findByIdAndEnabledTrue(Long id);
    java.util.List<User> findByRoleAndEnabledTrue(Role role);
    java.util.List<User> findByEnabledTrue();
    
    // Pagination methods
    Page<User> findByEnabledTrue(Pageable pageable);
    Page<User> findByRoleAndEnabledTrue(Role role, Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE u.username = :username AND u.enabled = true")
    Optional<User> findActiveUserByUsername(@Param("username") String username);
}

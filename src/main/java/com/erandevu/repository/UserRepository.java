package com.erandevu.repository;

import com.erandevu.entity.User;
import com.erandevu.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Kullanıcı repository arayüzü.
 * Soft delete @Where annotasyonu ile otomatik yönetilir.
 * Gereksiz metod tekrarları kaldırılmıştır - sadece sayfalama kullanılır.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Kullanıcı adına göre kullanıcı bulur.
     * Soft delete otomatik filtrelenir (@Where clause).
     */
    Optional<User> findByUsername(String username);

    /**
     * Email'e göre kullanıcı bulur.
     * Soft delete otomatik filtrelenir.
     */
    Optional<User> findByEmail(String email);

    /**
     * Kullanıcı adının varlığını kontrol eder.
     * Soft delete otomatik filtrelenir.
     */
    boolean existsByUsername(String username);

    /**
     * Email'in varlığını kontrol eder.
     * Soft delete otomatik filtrelenir.
     */
    boolean existsByEmail(String email);

    /**
     * Aktif kullanıcıyı kullanıcı adına göre bulur.
     * Soft delete otomatik filtrelenir.
     */
    Optional<User> findByUsernameAndEnabledTrue(String username);

    /**
     * Rol ve aktiflik durumuna göre kullanıcıları getirir (List versiyonu).
     * Soft delete otomatik filtrelenir.
     */
    java.util.List<User> findByRoleAndEnabledTrue(Role role);

    /**
     * Rol ve aktiflik durumuna göre kullanıcıları getirir (Sayfalama versiyonu).
     * Soft delete otomatik filtrelenir.
     */
    Page<User> findByRoleAndEnabledTrue(Role role, Pageable pageable);

    /**
     * Aktif tüm kullanıcıları sayfalama ile getirir.
     * Soft delete otomatik filtrelenir.
     */
    Page<User> findByEnabledTrue(Pageable pageable);
}

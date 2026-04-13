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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Kullanıcı servisi - Caching destekli, temiz yapı.
 * Soft delete otomatik olarak @Where annotasyonu ile yönetilir.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    /**
     * ID'ye göre kullanıcı getirir.
     *
     * @param id kullanıcı ID
     * @return kullanıcı yanıtı
     */
    @Cacheable(value = "users", key = "#id")
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return userMapper.toUserResponse(user);
    }

    /**
     * Kullanıcı adına göre kullanıcı getirir.
     *
     * @param username kullanıcı adı
     * @return kullanıcı yanıtı
     */
    @Cacheable(value = "users", key = "'username_' + #username")
    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsernameAndEnabledTrue(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        return userMapper.toUserResponse(user);
    }

    /**
     * Tüm aktif kullanıcıları getirir (sayfalama ile).
     *
     * @param page sayfa numarası
     * @param size sayfa boyutu
     * @return sayfalanmış kullanıcı listesi
     */
    @Cacheable(value = "users", key = "'all_' + #page + '_' + #size")
    public PageResponse<UserResponse> getAllUsers(int page, int size, String sortBy, String sortDir) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage = userRepository.findByEnabledTrue(pageable);
        Page<UserResponse> userResponsePage = userMapper.toUserResponsePage(userPage);
        return PageResponseUtil.createPageResponse(userResponsePage);
    }

    /**
     * Rol bazlı kullanıcıları getirir (sayfalama ile).
     *
     * @param role kullanıcı rolü
     * @param page sayfa numarası
     * @param size sayfa boyutu
     * @return sayfalanmış kullanıcı listesi
     */
    @Cacheable(value = "users", key = "'role_' + #role + '_' + #page + '_' + #size")
    public PageResponse<UserResponse> getUsersByRole(Role role, int page, int size, String sortBy, String sortDir) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage = userRepository.findByRoleAndEnabledTrue(role, pageable);
        Page<UserResponse> userResponsePage = userMapper.toUserResponsePage(userPage);
        return PageResponseUtil.createPageResponse(userResponsePage);
    }

    /**
     * Rol bazlı kullanıcıları getirir (list versiyonu).
     *
     * @param role kullanıcı rolü
     * @return kullanıcı listesi
     */
    @Cacheable(value = "users", key = "'role_list_' + #role")
    public List<UserResponse> getUsersByRole(Role role) {
        List<User> users = userRepository.findByRoleAndEnabledTrue(role);
        return users.stream()
                .map(userMapper::toUserResponse)
                .toList();
    }

    /**
     * Tüm doktorları getirir.
     */
    public List<UserResponse> getAllDoctors() {
        return getUsersByRole(Role.DOCTOR);
    }

    /**
     * Tüm doktorları getirir (sayfalama ile).
     */
    public PageResponse<UserResponse> getAllDoctorsPaginated(int page, int size, String sortBy, String sortDir) {
        return getUsersByRole(Role.DOCTOR, page, size, sortBy, sortDir);
    }

    /**
     * Tüm hastaları getirir.
     */
    public List<UserResponse> getAllPatients() {
        return getUsersByRole(Role.PATIENT);
    }

    /**
     * Tüm hastaları getirir (sayfalama ile).
     */
    public PageResponse<UserResponse> getAllPatientsPaginated(int page, int size, String sortBy, String sortDir) {
        return getUsersByRole(Role.PATIENT, page, size, sortBy, sortDir);
    }

    /**
     * Kullanıcı durumunu aktif/pasif yapar.
     * Cache temizlenir.
     *
     * @param id kullanıcı ID
     * @return güncellenmiş kullanıcı
     */
    @CacheEvict(value = "users", allEntries = true)
    public UserResponse toggleUserStatus(Long id) {
        log.info("Toggling user status: id={}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        user.setEnabled(!user.getEnabled());
        User updatedUser = userRepository.save(user);

        log.info("User status toggled: id={}, enabled={}", id, updatedUser.getEnabled());
        return userMapper.toUserResponse(updatedUser);
    }
}

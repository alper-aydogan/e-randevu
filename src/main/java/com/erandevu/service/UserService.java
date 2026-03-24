package com.erandevu.service;

import com.erandevu.dto.response.UserResponse;
import com.erandevu.entity.User;
import com.erandevu.enums.Role;
import com.erandevu.exception.ResourceNotFoundException;
import com.erandevu.mapper.UserMapper;
import com.erandevu.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findByIdAndEnabledTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return userMapper.toUserResponse(user);
    }

    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsernameAndEnabledTrue(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        return userMapper.toUserResponse(user);
    }

    public List<UserResponse> getAllUsers() {
        List<User> users = userRepository.findByEnabledTrue();
        return users.stream()
                .map(userMapper::toUserResponse)
                .toList();
    }

    public List<UserResponse> getUsersByRole(Role role) {
        List<User> users = userRepository.findByRoleAndEnabledTrue(role);
        return users.stream()
                .map(userMapper::toUserResponse)
                .toList();
    }

    public List<UserResponse> getAllDoctors() {
        return getUsersByRole(Role.DOCTOR);
    }

    public List<UserResponse> getAllPatients() {
        return getUsersByRole(Role.PATIENT);
    }

    public UserResponse toggleUserStatus(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        
        user.setEnabled(!user.getEnabled());
        User updatedUser = userRepository.save(user);
        
        return userMapper.toUserResponse(updatedUser);
    }
}

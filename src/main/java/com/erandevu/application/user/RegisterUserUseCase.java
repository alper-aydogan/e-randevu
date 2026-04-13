package com.erandevu.application.user;

import com.erandevu.dto.request.RegisterRequest;
import com.erandevu.dto.response.AuthResponse;
import com.erandevu.entity.User;
import com.erandevu.exception.UserAlreadyExistsException;
import com.erandevu.mapper.UserMapper;
import com.erandevu.repository.UserRepository;
import com.erandevu.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application Use Case: Register User.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RegisterUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    public AuthResponse execute(RegisterRequest request) {
        log.info("UC: Registering user - username={}", request.getUsername());

        // 1. Validate uniqueness
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists");
        }

        // 2. Create user
        User user = userMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(true);

        // 3. Persist
        User saved = userRepository.save(user);
        log.info("UC: User registered - id={}", saved.getId());

        // 4. Generate token
        String token = jwtService.generateToken(saved);

        // 5. Return response
        return AuthResponse.builder()
            .token(token)
            .id(saved.getId())
            .username(saved.getUsername())
            .email(saved.getEmail())
            .firstName(saved.getFirstName())
            .lastName(saved.getLastName())
            .role(saved.getRole().name())
            .build();
    }
}

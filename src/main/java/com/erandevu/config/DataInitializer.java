package com.erandevu.config;

import com.erandevu.enums.Role;
import com.erandevu.entity.User;
import com.erandevu.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile({"local", "dev"})
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner initData() {
        return args -> {
            log.info("Initializing database with sample data...");
            
            // Create Admin User
            if (userRepository.count() == 0) {
                User admin = User.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("admin123"))
                        .email("admin@erandevu.com")
                        .firstName("Admin")
                        .lastName("User")
                        .phoneNumber("+9055550101")
                        .role(Role.ADMIN)
                        .enabled(true)
                        .build();
                
                userRepository.save(admin);
                log.info("Created admin user: admin/admin123");
                
                // Create Doctor Users
                for (int i = 1; i <= 3; i++) {
                    User doctor = User.builder()
                            .username("dr_" + i)
                            .password(passwordEncoder.encode("password" + i))
                            .email("dr" + i + "@erandevu.com")
                            .firstName("Doctor")
                            .lastName("" + i)
                            .phoneNumber("+905555010" + (1 + i))
                            .role(Role.DOCTOR)
                            .enabled(true)
                            .build();
                    
                    userRepository.save(doctor);
                    log.info("Created doctor user: dr_{}/password{}", i, i);
                }
                
                // Create Patient Users
                for (int i = 1; i <= 5; i++) {
                    User patient = User.builder()
                            .username("patient_" + i)
                            .password(passwordEncoder.encode("password" + i))
                            .email("patient" + i + "@erandevu.com")
                            .firstName("Patient")
                            .lastName("" + i)
                            .phoneNumber("+905555020" + (1 + i))
                            .role(Role.PATIENT)
                            .enabled(true)
                            .build();
                    
                    userRepository.save(patient);
                    log.info("Created patient user: patient_{}/password{}", i, i);
                }
                
                log.info("Database initialization completed!");
                log.info("Total users created: {}", userRepository.count());
            } else {
                log.info("Database already contains data. Skipping initialization.");
            }
        };
    }
}

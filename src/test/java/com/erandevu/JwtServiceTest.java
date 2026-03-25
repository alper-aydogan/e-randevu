package com.erandevu;

import com.erandevu.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        
        // Test için JWT secret ve expiration değerlerini ayarla
        ReflectionTestUtils.setField(jwtService, "jwtSecret", "bXktc2VjcmV0LWtleS13aGljaC1pcy12ZXJ5LXNlY3VyZS1hbmQtbG9uZy1lbm91Z2gtZm9yLWhzaC1zaGEyNTY=");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3600000L); // 1 saat
        
        // Test kullanıcısı oluştur
        userDetails = User.builder()
                .username("testuser")
                .password("password")
                .roles("USER")
                .build();
    }

    @Test
    void testGenerateToken() {
        String token = jwtService.generateToken(userDetails);
        
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3); // JWT formatı: header.payload.signature
    }

    @Test
    void testGenerateTokenWithExtraClaims() {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", "USER");
        extraClaims.put("department", "IT");
        
        String token = jwtService.generateToken(extraClaims, userDetails);
        
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void testExtractUsername() {
        String token = jwtService.generateToken(userDetails);
        String extractedUsername = jwtService.extractUsername(token);
        
        assertEquals(userDetails.getUsername(), extractedUsername);
    }

    @Test
    void testExtractExpiration() {
        String token = jwtService.generateToken(userDetails);
        Date expirationDate = jwtService.extractExpiration(token);
        
        assertNotNull(expirationDate);
        assertTrue(expirationDate.after(new Date()));
    }

    @Test
    void testExtractClaim() {
        String role = "ADMIN";
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", role);
        
        String token = jwtService.generateToken(extraClaims, userDetails);
        String extractedRole = jwtService.extractClaim(token, claims -> claims.get("role", String.class));
        
        assertEquals(role, extractedRole);
    }

    @Test
    void testIsTokenValid() {
        String token = jwtService.generateToken(userDetails);
        
        assertTrue(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    void testIsTokenValid_WithWrongUser() {
        String token = jwtService.generateToken(userDetails);
        
        UserDetails differentUser = User.builder()
                .username("differentuser")
                .password("password")
                .roles("USER")
                .build();
        
        assertFalse(jwtService.isTokenValid(token, differentUser));
    }

    @Test
    void testExtractUsername_WithInvalidToken() {
        String invalidToken = "invalid.token.here";
        
        assertThrows(Exception.class, () -> jwtService.extractUsername(invalidToken));
    }

    @Test
    void testTokenExpiration() {
        // Kısa süreli token oluştur
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 1L); // 1 milisaniye
        String token = jwtService.generateToken(userDetails);
        
        // Token'ın süresi geçene kadar bekle
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Token süresi geçtiğinde isTokenValid methodu exception fırlatmalı
        assertThrows(Exception.class, () -> jwtService.isTokenValid(token, userDetails));
    }

    @Test
    void testGenerateToken_DifferentUsersGenerateDifferentTokens() {
        UserDetails user1 = User.builder()
                .username("user1")
                .password("password")
                .roles("USER")
                .build();
        
        UserDetails user2 = User.builder()
                .username("user2")
                .password("password")
                .roles("USER")
                .build();
        
        String token1 = jwtService.generateToken(user1);
        String token2 = jwtService.generateToken(user2);
        
        assertNotEquals(token1, token2);
        assertEquals("user1", jwtService.extractUsername(token1));
        assertEquals("user2", jwtService.extractUsername(token2));
    }
}

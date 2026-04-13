package com.erandevu.security;

import com.erandevu.service.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Production-grade JWT authentication filter with:
 * - Fail-safe design (never breaks request chain)
 * - Early token validation before DB calls
 * - Comprehensive exception handling
 * - Security context cleanup on failure
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int BEARER_PREFIX_LENGTH = 7;

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        // Step 1: Check if Authorization header is present and valid
        if (!isValidAuthHeader(authHeader)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(BEARER_PREFIX_LENGTH);

        // Step 2: Early validation - check token structure before DB calls
        if (!isTokenStructurallyValid(jwt)) {
            log.debug("JWT token structure validation failed for request: {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Step 3: Extract username safely
            final String username = jwtService.extractUsername(jwt);

            if (!StringUtils.hasText(username)) {
                log.warn("JWT token contains no username");
                filterChain.doFilter(request, response);
                return;
            }

            // Step 4: Check if already authenticated
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                filterChain.doFilter(request, response);
                return;
            }

            // Step 5: Load user details (DB call)
            UserDetails userDetails = loadUserDetails(username);
            if (userDetails == null) {
                filterChain.doFilter(request, response);
                return;
            }

            // Step 6: Validate token signature and expiration
            if (!jwtService.isTokenValid(jwt, userDetails)) {
                log.debug("JWT token validation failed for user: {}", username);
                filterChain.doFilter(request, response);
                return;
            }

            // Step 7: Create authentication token and set context
            UsernamePasswordAuthenticationToken authToken = createAuthenticationToken(
                    userDetails, request
            );
            SecurityContextHolder.getContext().setAuthentication(authToken);

            log.debug("Authenticated user: {}, URI: {}", username, request.getRequestURI());

        } catch (ExpiredJwtException e) {
            log.debug("JWT token expired: {}", e.getMessage());
            clearSecurityContext();
        } catch (SignatureException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
            clearSecurityContext();
        } catch (MalformedJwtException e) {
            log.debug("Malformed JWT token: {}", e.getMessage());
            clearSecurityContext();
        } catch (JwtException e) {
            log.debug("JWT processing error: {}", e.getMessage());
            clearSecurityContext();
        } catch (Exception e) {
            // CRITICAL: Never break the filter chain, log and continue
            log.error("Unexpected error during JWT authentication", e);
            clearSecurityContext();
        }

        // Always continue filter chain
        filterChain.doFilter(request, response);
    }

    /**
     * Validate Authorization header format.
     */
    private boolean isValidAuthHeader(String authHeader) {
        return StringUtils.hasText(authHeader)
                && authHeader.startsWith(BEARER_PREFIX)
                && authHeader.length() > BEARER_PREFIX_LENGTH;
    }

    /**
     * Early structural validation to avoid unnecessary DB calls.
     * Checks if token has valid JWT structure (3 parts separated by dots).
     */
    private boolean isTokenStructurallyValid(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }
        // JWT should have 3 parts: header.payload.signature
        String[] parts = token.split("\\.");
        return parts.length == 3;
    }

    /**
     * Load user details with proper exception handling.
     */
    private UserDetails loadUserDetails(String username) {
        try {
            return userDetailsService.loadUserByUsername(username);
        } catch (UsernameNotFoundException e) {
            log.debug("User not found: {}", username);
            return null;
        } catch (Exception e) {
            log.error("Error loading user details for: {}", username, e);
            return null;
        }
    }

    /**
     * Create authentication token with request details.
     */
    private UsernamePasswordAuthenticationToken createAuthenticationToken(
            UserDetails userDetails, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        return authToken;
    }

    /**
     * Clear security context to prevent authentication leakage.
     */
    private void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }
}

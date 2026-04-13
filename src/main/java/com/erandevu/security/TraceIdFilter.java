package com.erandevu.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter to add trace ID to every request for distributed tracing and observability.
 * Runs first in the filter chain (Ordered.HIGHEST_PRECEDENCE).
 * Adds trace ID to:
 * - MDC (Mapped Diagnostic Context) for logging
 * - Response header for client-side tracking
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String TRACE_ID_MDC_KEY = "traceId";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Generate or extract trace ID
        String traceId = extractOrGenerateTraceId(request);

        // Add to MDC for logging
        MDC.put(TRACE_ID_MDC_KEY, traceId);

        // Add to response header
        response.setHeader(TRACE_ID_HEADER, traceId);

        try {
            log.debug("Request started - TraceId: {}, Method: {}, URI: {}",
                    traceId, request.getMethod(), request.getRequestURI());

            filterChain.doFilter(request, response);

            log.debug("Request completed - TraceId: {}, Status: {}",
                    traceId, response.getStatus());

        } finally {
            // Clean up MDC to prevent thread pollution
            MDC.remove(TRACE_ID_MDC_KEY);
        }
    }

    /**
     * Extract trace ID from request header or generate new one.
     */
    private String extractOrGenerateTraceId(HttpServletRequest request) {
        String existingTraceId = request.getHeader(TRACE_ID_HEADER);

        if (existingTraceId != null && !existingTraceId.isBlank()) {
            log.debug("Using existing trace ID from request: {}", existingTraceId);
            return existingTraceId;
        }

        return UUID.randomUUID().toString();
    }
}

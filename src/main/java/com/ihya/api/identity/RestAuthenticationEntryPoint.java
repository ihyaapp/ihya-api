package com.ihya.api.identity;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Renders the project's standard {@link ErrorResponse} JSON when an
 * unauthenticated request reaches a protected route.
 *
 * <p>{@link JwtAuthenticationFilter} never rejects a request itself — it only
 * populates the security context when a valid token is present. An anonymous (or
 * bad-token) request to a protected endpoint is therefore stopped by Spring
 * Security's authorization layer <em>before</em> it reaches a controller, so
 * {@link GlobalExceptionHandler} never sees it. Without this entry point the
 * client gets Spring Security's bare default 401 (empty body) instead of the
 * {@code {status,error,message,timestamp}} shape every other error uses.
 *
 * <p>The body is built with {@link ErrorResponse#of} — the exact same factory
 * {@link GlobalExceptionHandler} uses — so there is only one place in the
 * codebase that decides what an error body looks like. Serialisation goes
 * through the Spring-configured {@link ObjectMapper}, so the {@code timestamp}
 * is rendered identically to a {@code GlobalExceptionHandler} response.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(),
                ErrorResponse.of(HttpStatus.UNAUTHORIZED, "Authentication required"));
    }
}

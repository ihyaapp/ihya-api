package com.ihya.api.common.web;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Renders the project's standard {@link ErrorResponse} JSON when an
 * authenticated request is rejected by an authorization check it doesn't
 * satisfy — e.g. a non-{@code ADMIN} user hitting a
 * {@code @PreAuthorize("hasRole('ADMIN')")} catalogue write.
 *
 * <p>Method security throws {@link AccessDeniedException} before the request
 * reaches the controller method, so neither {@link GlobalExceptionHandler} nor
 * a module's own advice ever sees it. Without this handler the client gets
 * Spring Security's bare default 403 (empty body) instead of the
 * {@code {status,error,message,timestamp}} shape every other error uses.
 * Mirrors {@link RestAuthenticationEntryPoint}, which does the same job for
 * the 401 case.
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(),
                ErrorResponse.of(HttpStatus.FORBIDDEN, "You do not have permission to perform this action"));
    }
}

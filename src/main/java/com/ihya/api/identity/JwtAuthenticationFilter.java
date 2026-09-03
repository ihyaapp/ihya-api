package com.ihya.api.identity;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Reads an {@code Authorization: Bearer <jwt>} header on every incoming request
 * and, when the token is valid, populates the {@link SecurityContextHolder}
 * with an authentication for the token's user id.
 *
 * <p>This filter never rejects a request. A missing, malformed, or
 * invalid/expired token simply leaves the request unauthenticated and the
 * chain continues: public endpoints such as {@code /auth/**} carry no
 * Authorization header at all, and that is not an error here. Enforcing
 * authentication on protected routes is the {@link SecurityConfig} filter
 * chain's job, not this filter's.
 *
 * <p>There is no role or permission model in the project yet, so the resulting
 * {@link UsernamePasswordAuthenticationToken} carries the user id as its
 * principal and an empty authority list.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractBearerToken(request);

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UUID userId = jwtService.extractUserId(token);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, List.of());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                // Invalid / expired / tampered token: do not authenticate and do
                // not reject. The security filter chain decides whether this
                // request is allowed through unauthenticated.
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private static String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }
}

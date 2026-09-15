package com.ihya.api.identity;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
 * <p>The resulting {@link UsernamePasswordAuthenticationToken} carries the
 * user id as its principal and a single {@code ROLE_<Role>} authority (e.g.
 * {@code ROLE_ADMIN}), looked up fresh from {@link UserRepository} on every
 * request rather than embedded in the JWT — a demoted admin loses write access
 * to the catalogue immediately, instead of only once their 15-minute access
 * token expires. {@code @EnableMethodSecurity} plus
 * {@code @PreAuthorize("hasRole('ADMIN')")} on the catalogue write endpoints
 * is what actually reads this authority (see {@link SecurityConfig}). A token
 * whose user id no longer exists (account deleted after the token was issued)
 * authenticates with no authorities, same as before this lookup existed.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractBearerToken(request);

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UUID userId = jwtService.extractUserId(token);
                List<GrantedAuthority> authorities = authoritiesFor(userId);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, authorities);
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

    private List<GrantedAuthority> authoritiesFor(UUID userId) {
        return userRepository.findById(userId)
                .<List<GrantedAuthority>>map(user -> List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())))
                .orElse(List.of());
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

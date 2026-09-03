package com.ihya.api.identity;

import com.ihya.api.common.web.RestAuthenticationEntryPoint;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Stateless JWT security chain.
     *
     * <ul>
     *   <li>CSRF disabled — there are no browser sessions or cookies to protect;
     *       every request authenticates from scratch via a bearer token.</li>
     *   <li>No HTTP session is ever created or used ({@code STATELESS}).</li>
     *   <li>{@code /auth/**} is open (register, login, refresh); everything else
     *       requires an authenticated request.</li>
     *   <li>Internal {@code ERROR} dispatches are not subject to authorization:
     *       the original request was already checked, and without this an
     *       unhandled exception on a public endpoint (e.g. a bean-validation
     *       400) is re-dispatched to {@code /error} and comes back as a
     *       misleading 403.</li>
     *   <li>{@link JwtAuthenticationFilter} runs before the username/password
     *       filter so a valid bearer token is already in the
     *       {@code SecurityContext} by the time authorization is checked.</li>
     *   <li>{@link RestAuthenticationEntryPoint} turns an unauthenticated hit on
     *       a protected route into the project's standard
     *       {@link com.ihya.api.common.web.ErrorResponse} JSON body, instead of
     *       Spring Security's bare default 401.</li>
     * </ul>
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtService jwtService,
                                                   RestAuthenticationEntryPoint authenticationEntryPoint) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/auth/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint))
                .addFilterBefore(new JwtAuthenticationFilter(jwtService),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

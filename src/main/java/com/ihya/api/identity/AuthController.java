package com.ihya.api.identity;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * HTTP entry points for the identity module: user registration, login and
 * refresh-token rotation.
 *
 * <p>All three endpoints respond with an {@link AuthResponse}: a newly
 * registered user is auto-logged-in and does not need a separate
 * {@code /auth/login} call, and a successful {@code /auth/refresh} returns a
 * fresh access/refresh token pair.
 *
 * <p>By design there is NO exception handling here. {@link EmailAlreadyRegisteredException},
 * {@link InvalidCredentialsException} and {@link InvalidRefreshTokenException}
 * are left to propagate uncaught. Until the later {@code @ControllerAdvice}
 * commit that maps them to HTTP 409 / 401 / 401, hitting these endpoints with a
 * duplicate email, bad credentials or an invalid refresh token surfaces as a
 * raw HTTP 500.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(UserService userService, RefreshTokenService refreshTokenService) {
        this.userService = userService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        RegistrationResult result = userService.register(request.email(), request.password());
        return toAuthResponse(result.user().getId(), result.tokens());
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        LoginResult result = userService.login(request.email(), request.password());
        return toAuthResponse(result.user().getId(), result.tokens());
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        RefreshResult result = refreshTokenService.validateAndRotate(request.refreshToken());
        return toAuthResponse(result.userId(), result.tokens());
    }

    /**
     * Maps a user id + service-layer {@link AuthTokens} pair onto the wire
     * contract, converting the access-token lifetime from minutes
     * ({@link AuthTokens}) to seconds ({@link AuthResponse#expiresIn()}).
     */
    private static AuthResponse toAuthResponse(UUID userId, AuthTokens tokens) {
        long expiresInSeconds = tokens.accessTokenExpiryMinutes() * 60;
        return new AuthResponse(
                tokens.accessToken(),
                expiresInSeconds,
                tokens.refreshToken(),
                userId);
    }
}

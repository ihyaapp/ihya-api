package com.ihya.api.identity;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP entry points for the identity module: user registration and login.
 *
 * <p>Both endpoints respond with an {@link AuthResponse}: a newly registered
 * user is auto-logged-in and does not need a separate {@code /auth/login} call.
 *
 * <p>By design there is NO exception handling here. {@link EmailAlreadyRegisteredException}
 * and {@link InvalidCredentialsException} are left to propagate uncaught.
 * Until the later {@code @ControllerAdvice} commit that maps them to HTTP 409
 * and 401, hitting these endpoints with a duplicate email or bad credentials
 * surfaces as a raw HTTP 500.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        RegistrationResult result = userService.register(request.email(), request.password());
        return toAuthResponse(result.user(), result.tokens());
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        LoginResult result = userService.login(request.email(), request.password());
        return toAuthResponse(result.user(), result.tokens());
    }

    /**
     * Maps the service-layer {@link User} + {@link AuthTokens} pair onto the
     * wire contract, converting the access-token lifetime from minutes
     * ({@link AuthTokens}) to seconds ({@link AuthResponse#expiresIn()}).
     */
    private static AuthResponse toAuthResponse(User user, AuthTokens tokens) {
        long expiresInSeconds = tokens.accessTokenExpiryMinutes() * 60;
        return new AuthResponse(
                tokens.accessToken(),
                expiresInSeconds,
                tokens.refreshToken(),
                user.getId());
    }
}

package com.ihya.api.identity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Read access to the currently authenticated user.
 *
 * <p>Kept separate from {@link AuthController} (which is {@code /auth}-scoped and
 * only handles the token lifecycle); {@code /me} is an authenticated read of the
 * caller's own record.
 *
 * <p>{@link JwtAuthenticationFilter} puts the token's user id straight into the
 * security context as the principal — a raw {@link UUID}, not a
 * {@code UserDetails} — so {@code @AuthenticationPrincipal} binds a {@link UUID}
 * here directly.
 *
 * <p>An unauthenticated request never reaches this method: the security filter
 * chain stops it first and {@link RestAuthenticationEntryPoint} renders the 401.
 */
@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal UUID userId) {
        return MeResponse.from(userService.getById(userId));
    }
}

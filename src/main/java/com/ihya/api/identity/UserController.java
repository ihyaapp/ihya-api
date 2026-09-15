package com.ihya.api.identity;

import com.ihya.api.profile.Profile;
import com.ihya.api.profile.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
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
 * chain stops it first and {@link com.ihya.api.common.web.RestAuthenticationEntryPoint}
 * renders the 401.
 *
 * <p>{@code /me} composes identity and profile data: identity owns id/email/
 * timezone/createdAt, profile owns name/interests/personalizePromptDismissed —
 * this controller stitches the two together via {@link UserService} and
 * {@link ProfileService} rather than either module reaching into the other's tables.
 */
@RestController
public class UserController {

    private final UserService userService;
    private final ProfileService profileService;

    public UserController(UserService userService, ProfileService profileService) {
        this.userService = userService;
        this.profileService = profileService;
    }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal UUID userId) {
        User user = userService.getById(userId);
        Profile profile = profileService.getProfile(userId);
        List<String> interests = profileService.getInterestSlugs(userId);
        return MeResponse.from(user, profile, interests);
    }

    @PatchMapping("/me")
    public MeResponse updateMe(@AuthenticationPrincipal UUID userId, @Valid @RequestBody UpdateMeRequest request) {
        UpdateMeResult result = userService.updateMe(userId, request);
        return MeResponse.from(result.user(), result.profile(), result.interests());
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMe(@AuthenticationPrincipal UUID userId) {
        userService.deleteMe(userId);
        return ResponseEntity.accepted().build();
    }
}

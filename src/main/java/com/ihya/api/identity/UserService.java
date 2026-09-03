package com.ihya.api.identity;

import com.ihya.api.profile.ProfileService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final ProfileService profileService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final JwtProperties jwtProperties;


    public UserService(UserRepository userRepository,
                       ProfileService profileService,
                       PasswordEncoder passwordEncoder,JwtService jwtService,
                       RefreshTokenService refreshTokenService,
                       JwtProperties jwtProperties) {
        this.userRepository = userRepository;
        this.profileService = profileService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public RegistrationResult register(String email, String rawPassword) {
        String normalizedEmail = normalizeEmail(email);

        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new EmailAlreadyRegisteredException(normalizedEmail);
        }

        String hashedPassword = passwordEncoder.encode(rawPassword);
        User user = new User(normalizedEmail, hashedPassword);
        User savedUser;
        try {
            // saveAndFlush (not save): force the INSERT — and any unique-constraint
            // violation — to happen here, inside the try, rather than being
            // deferred to transaction commit after this method has returned.
            savedUser = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            // A concurrent registration for the same email can pass the check
            // above and then lose the race to the DB unique constraint. Remap
            // only that specific violation to the same exception the check-first
            // path throws; any other integrity error is real and must propagate.
            if (isEmailUniqueViolation(ex)) {
                throw new EmailAlreadyRegisteredException(normalizedEmail);
            }
            throw ex;
        }

        profileService.createProfile(savedUser.getId());

        // Auto-login the new user: issue tokens directly rather than calling
        // login(), which would re-run BCrypt against the hash we just created.
        String accessToken = jwtService.generateAccessToken(savedUser.getId());
        String refreshToken = refreshTokenService.issueRefreshToken(savedUser.getId());

        AuthTokens tokens = new AuthTokens(accessToken, refreshToken, jwtProperties.getAccessTokenExpiryMinutes());
        return new RegistrationResult(savedUser, tokens);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(normalizeEmail(email));
    }

    /**
     * Loads the user behind an authenticated request. The id comes from a
     * verified access token, so a miss here means the account was deleted after
     * the token was issued — surfaced as {@link UserNotFoundException} (a 401,
     * see that type for why).
     */
    public User getById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
    }
    public LoginResult login(String email, String rawPassword) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String accessToken = jwtService.generateAccessToken(user.getId());
        String refreshToken = refreshTokenService.issueRefreshToken(user.getId());

        AuthTokens tokens = new AuthTokens(accessToken, refreshToken, jwtProperties.getAccessTokenExpiryMinutes());
        return new LoginResult(user, tokens);
    }

    /**
     * Canonical form of an email for both storage and lookup: surrounding
     * whitespace trimmed, then lower-cased. The {@code users.email} unique
     * constraint is case-sensitive at the DB level (plain {@code VARCHAR}, see
     * {@code V2__create_identity_and_profile_tables.sql}), so it is the
     * application that makes email uniqueness — and therefore login — behave
     * case-insensitively. Every write and every lookup must pass through here.
     */
    private static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * True only when the given violation is the {@code users.email} unique
     * constraint. {@code users} has exactly one other constraint — the
     * {@code id} primary key — and masking an unrelated integrity error as a
     * duplicate email would hide a real bug, so this matches specifically on the
     * constraint name Postgres derives for the {@code email} column
     * ({@code users_email_key}).
     */
    private static boolean isEmailUniqueViolation(DataIntegrityViolationException ex) {
        Throwable cause = ex.getMostSpecificCause();
        String message = cause == null ? null : cause.getMessage();
        return message != null && message.toLowerCase(Locale.ROOT).contains("users_email_key");
    }
}
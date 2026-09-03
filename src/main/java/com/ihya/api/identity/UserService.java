package com.ihya.api.identity;

import com.ihya.api.profile.ProfileService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        if (userRepository.findByEmail(email).isPresent()) {
            throw new EmailAlreadyRegisteredException(email);
        }

        String hashedPassword = passwordEncoder.encode(rawPassword);
        User user = new User(email, hashedPassword);
        User savedUser = userRepository.save(user);

        profileService.createProfile(savedUser.getId());

        // Auto-login the new user: issue tokens directly rather than calling
        // login(), which would re-run BCrypt against the hash we just created.
        String accessToken = jwtService.generateAccessToken(savedUser.getId());
        String refreshToken = refreshTokenService.issueRefreshToken(savedUser.getId());

        AuthTokens tokens = new AuthTokens(accessToken, refreshToken, jwtProperties.getAccessTokenExpiryMinutes());
        return new RegistrationResult(savedUser, tokens);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
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
        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String accessToken = jwtService.generateAccessToken(user.getId());
        String refreshToken = refreshTokenService.issueRefreshToken(user.getId());

        AuthTokens tokens = new AuthTokens(accessToken, refreshToken, jwtProperties.getAccessTokenExpiryMinutes());
        return new LoginResult(user, tokens);
    }
}
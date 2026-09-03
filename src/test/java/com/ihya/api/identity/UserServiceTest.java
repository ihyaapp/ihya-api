package com.ihya.api.identity;

import com.ihya.api.profile.ProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link UserService}: every collaborator is a Mockito mock
 * and the service is constructed by hand, so no Spring context and no database
 * are involved.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ProfileService profileService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private JwtProperties jwtProperties;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, profileService, passwordEncoder,
                jwtService, refreshTokenService, jwtProperties);
    }

    // ----------------------------------------------------------------------
    // register()
    // ----------------------------------------------------------------------

    @Test
    void register_newEmail_savesUserIssuesTokensAndReturnsResult() {
        String email = "new@example.com";
        String rawPassword = "s3cret-raw";
        UUID newId = UUID.randomUUID();
        User savedUser = userWithId(newId, email, "hashed-pw");
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(rawPassword)).thenReturn("hashed-pw");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateAccessToken(newId)).thenReturn("access-token");
        when(refreshTokenService.issueRefreshToken(newId)).thenReturn("refresh-token");
        when(jwtProperties.getAccessTokenExpiryMinutes()).thenReturn(15L);

        RegistrationResult result = userService.register(email, rawPassword);

        verify(userRepository).save(any(User.class));
        verify(profileService).createProfile(newId);
        verify(jwtService).generateAccessToken(newId);
        verify(refreshTokenService).issueRefreshToken(newId);
        assertThat(result.user()).isSameAs(savedUser);
        assertThat(result.tokens().accessToken()).isEqualTo("access-token");
        assertThat(result.tokens().refreshToken()).isEqualTo("refresh-token");
        assertThat(result.tokens().accessTokenExpiryMinutes()).isEqualTo(15L);
    }

    @Test
    void register_newEmail_hashesPasswordBeforeSaving() {
        String email = "hash-check@example.com";
        String rawPassword = "plaintext-password";
        UUID newId = UUID.randomUUID();
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(rawPassword)).thenReturn("bcrypt$hash$value");
        when(userRepository.save(any(User.class))).thenReturn(userWithId(newId, email, "bcrypt$hash$value"));
        when(jwtService.generateAccessToken(newId)).thenReturn("access-token");
        when(refreshTokenService.issueRefreshToken(newId)).thenReturn("refresh-token");
        when(jwtProperties.getAccessTokenExpiryMinutes()).thenReturn(15L);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        userService.register(email, rawPassword);

        verify(passwordEncoder).encode(rawPassword);
        verify(userRepository).save(userCaptor.capture());
        User persisted = userCaptor.getValue();
        assertThat(persisted.getEmail()).isEqualTo(email);
        assertThat(persisted.getPasswordHash())
                .isNotEqualTo(rawPassword)
                .isEqualTo("bcrypt$hash$value");
    }

    @Test
    void register_duplicateEmail_throwsEmailAlreadyRegisteredExceptionAndNeverSaves() {
        String email = "taken@example.com";
        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(userWithId(UUID.randomUUID(), email, "existing-hash")));

        Throwable thrown = catchThrowable(() -> userService.register(email, "any-password"));

        assertThat(thrown).isInstanceOf(EmailAlreadyRegisteredException.class);
        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder, profileService, jwtService, refreshTokenService);
    }

    // ----------------------------------------------------------------------
    // login()
    // ----------------------------------------------------------------------

    @Test
    void login_correctCredentials_issuesTokensAndReturnsUser() {
        String email = "member@example.com";
        String rawPassword = "correct-password";
        UUID userId = UUID.randomUUID();
        User user = userWithId(userId, email, "stored-hash");
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(rawPassword, "stored-hash")).thenReturn(true);
        when(jwtService.generateAccessToken(userId)).thenReturn("access-token");
        when(refreshTokenService.issueRefreshToken(userId)).thenReturn("refresh-token");
        when(jwtProperties.getAccessTokenExpiryMinutes()).thenReturn(15L);

        LoginResult result = userService.login(email, rawPassword);

        verify(passwordEncoder).matches(rawPassword, "stored-hash");
        verify(jwtService).generateAccessToken(userId);
        verify(refreshTokenService).issueRefreshToken(userId);
        assertThat(result.user()).isSameAs(user);
        assertThat(result.tokens().accessToken()).isEqualTo("access-token");
        assertThat(result.tokens().refreshToken()).isEqualTo("refresh-token");
        assertThat(result.tokens().accessTokenExpiryMinutes()).isEqualTo(15L);
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentialsExceptionAndIssuesNoTokens() {
        String email = "member@example.com";
        User user = userWithId(UUID.randomUUID(), email, "stored-hash");
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "stored-hash")).thenReturn(false);

        Throwable thrown = catchThrowable(() -> userService.login(email, "wrong-password"));

        assertThat(thrown).isInstanceOf(InvalidCredentialsException.class);
        verifyNoInteractions(jwtService, refreshTokenService);
    }

    @Test
    void login_unknownEmail_throwsInvalidCredentialsExceptionAndIssuesNoTokens() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        Throwable thrown = catchThrowable(() -> userService.login("ghost@example.com", "any-password"));

        assertThat(thrown).isInstanceOf(InvalidCredentialsException.class);
        verifyNoInteractions(passwordEncoder, jwtService, refreshTokenService);
    }

    @Test
    void login_unknownEmailAndWrongPassword_throwSameExceptionTypeAndMessage() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());
        User existing = userWithId(UUID.randomUUID(), "real@example.com", "stored-hash");
        when(userRepository.findByEmail("real@example.com")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("bad", "stored-hash")).thenReturn(false);

        Throwable unknownEmail = catchThrowable(() -> userService.login("ghost@example.com", "bad"));
        Throwable wrongPassword = catchThrowable(() -> userService.login("real@example.com", "bad"));

        assertThat(unknownEmail).isExactlyInstanceOf(InvalidCredentialsException.class);
        assertThat(wrongPassword).isExactlyInstanceOf(InvalidCredentialsException.class);
        assertThat(unknownEmail).hasMessage(wrongPassword.getMessage());
    }

    // ----------------------------------------------------------------------
    // getById() — backs GET /me
    // ----------------------------------------------------------------------

    @Test
    void getById_existingId_returnsUser() {
        UUID userId = UUID.randomUUID();
        User user = userWithId(userId, "me@example.com", "stored-hash");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        User result = userService.getById(userId);

        assertThat(result).isSameAs(user);
        assertThat(result.getId()).isEqualTo(userId);
        assertThat(result.getEmail()).isEqualTo("me@example.com");
    }

    @Test
    void getById_unknownId_throwsUserNotFoundException() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        Throwable thrown = catchThrowable(() -> userService.getById(userId));

        assertThat(thrown).isInstanceOf(UserNotFoundException.class);
        verifyNoInteractions(jwtService, refreshTokenService, profileService);
    }

    // ----------------------------------------------------------------------
    // findByEmail()
    // ----------------------------------------------------------------------

    @Test
    void findByEmail_existingEmail_returnsUserFromRepository() {
        User user = userWithId(UUID.randomUUID(), "known@example.com", "stored-hash");
        when(userRepository.findByEmail("known@example.com")).thenReturn(Optional.of(user));

        Optional<User> result = userService.findByEmail("known@example.com");

        verify(userRepository).findByEmail("known@example.com");
        assertThat(result).containsSame(user);
    }

    @Test
    void findByEmail_unknownEmail_returnsEmptyOptional() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        Optional<User> result = userService.findByEmail("nobody@example.com");

        verify(userRepository).findByEmail("nobody@example.com");
        assertThat(result).isEmpty();
    }

    // ----------------------------------------------------------------------
    // helpers
    // ----------------------------------------------------------------------

    /**
     * Builds a {@link User} with its generated {@code id} populated. {@code User}
     * has no id setter (Hibernate assigns it on persist), so tests simulate a
     * saved row by setting the field reflectively.
     */
    private static User userWithId(UUID id, String email, String passwordHash) {
        User user = new User(email, passwordHash);
        try {
            Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not set User.id for test fixture", e);
        }
        return user;
    }
}

package com.ihya.api.identity;

import com.ihya.api.profile.ProfileRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack integration tests for the authentication endpoints.
 *
 * <p>Unlike {@code UserServiceTest} (which mocks every collaborator), this class
 * boots the real Spring context and drives requests through the whole chain with
 * {@link MockMvc}: the real {@code SecurityFilterChain}, {@link JwtAuthenticationFilter},
 * {@link RestAuthenticationEntryPoint}, {@link GlobalExceptionHandler} and a real
 * Postgres database. Nothing is mocked; the service layer runs for real.
 *
 * <h2>Database</h2>
 * Uses the {@code ci} profile — the same convention as {@code IhyaApiApplicationTests}
 * and the CI workflow. {@code application-ci.yml} points at a Postgres on
 * {@code localhost:5432} and Flyway migrates it on context start. CI supplies that
 * Postgres as a service container ({@code .github/workflows/ci.yml}); locally the
 * {@code docker-compose.yml} {@code ihya-postgres} container serves the same role.
 * Running locally therefore needs {@code DB_PASSWORD} set, exactly as the existing
 * context test already requires. No Testcontainers — it would duplicate the
 * infrastructure {@code application-ci.yml} and the CI service container already
 * provide.
 *
 * <h2>Isolation</h2>
 * Every test starts and ends against empty {@code refresh_tokens} / {@code profiles}
 * / {@code users} tables via {@link #resetDatabase()}. Explicit cleanup is used
 * rather than {@code @Transactional} test rollback because:
 * <ul>
 *   <li>{@link UserService#register} is itself {@code @Transactional} and throws
 *       {@link EmailAlreadyRegisteredException} <em>inside</em> its own transaction.
 *       Under a shared test transaction that marks the whole test rollback-only,
 *       making any "successful write then failing register" test brittle.</li>
 *   <li>A test transaction that never commits cannot catch failures that only
 *       surface at commit (FK / unique / not-null). A full-stack test should
 *       commit for real, the way the app does.</li>
 * </ul>
 * Cleanup runs {@code @BeforeEach} as well so a crashed test cannot poison the
 * next one and pre-existing rows never leak in.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("ci")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private ProfileRepository profileRepository;

    @BeforeEach
    @AfterEach
    void resetDatabase() {
        refreshTokenRepository.deleteAllInBatch();
        profileRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    // ------------------------------------------------------------------
    // Happy path — the whole flow in one test
    // ------------------------------------------------------------------

    @Test
    void fullAuthFlow_registerThenMeThenRefreshThenMeAgain_allSucceed() throws Exception {
        String email = "flow-user@example.com";
        String password = "correct-horse-battery";

        String registerBody = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(authJson(email, password)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.userId").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String accessToken1 = JsonPath.read(registerBody, "$.accessToken");
        String refreshToken1 = JsonPath.read(registerBody, "$.refreshToken");
        String userId = JsonPath.read(registerBody, "$.userId");

        mockMvc.perform(get("/me").header("Authorization", "Bearer " + accessToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        String refreshBody = mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson(refreshToken1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.userId").value(userId))
                .andReturn().getResponse().getContentAsString();
        String accessToken2 = JsonPath.read(refreshBody, "$.accessToken");
        String refreshToken2 = JsonPath.read(refreshBody, "$.refreshToken");

        assertThat(refreshToken2).isNotEqualTo(refreshToken1);
        mockMvc.perform(get("/me").header("Authorization", "Bearer " + accessToken2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.email").value(email));
    }

    // ------------------------------------------------------------------
    // Registration failures
    // ------------------------------------------------------------------

    @Test
    void register_duplicateEmail_returns409WithConflictErrorShape() throws Exception {
        String email = "dupe@example.com";
        register(email, "first-password");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(authJson(email, "second-password")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Email already registered: " + email))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void register_malformedJsonBody_returns400() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"x@y.com\", "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Request body is missing or is not valid JSON"));
    }

    @Test
    void register_invalidEmailAndShortPassword_returns400ListingBothFieldErrors() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(authJson("not-an-email", "short")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message", containsString("email:")))
                .andExpect(jsonPath("$.message", containsString("password: password must be at least 8 characters")))
                .andExpect(jsonPath("$.message", containsString("; ")));
    }

    // ------------------------------------------------------------------
    // Login failures — must not leak which of email / password was wrong
    // ------------------------------------------------------------------

    @Test
    void login_wrongPassword_returns401WithInvalidCredentialsShape() throws Exception {
        String email = "wrong-pw@example.com";
        register(email, "the-right-password");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(authJson(email, "the-wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void login_unknownEmail_returns401IdenticalToWrongPassword() throws Exception {
        String realEmail = "known@example.com";
        register(realEmail, "real-password");

        String wrongPasswordBody = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(authJson(realEmail, "bad-password")))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        String unknownEmailBody = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(authJson("nobody@example.com", "bad-password")))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        Map<String, Object> wrongPassword = JsonPath.read(wrongPasswordBody, "$");
        Map<String, Object> unknownEmail = JsonPath.read(unknownEmailBody, "$");
        assertThat(unknownEmail.keySet()).isEqualTo(wrongPassword.keySet());
        assertThat(unknownEmail.get("status")).isEqualTo(wrongPassword.get("status"));
        assertThat(unknownEmail.get("error")).isEqualTo(wrongPassword.get("error"));
        assertThat(unknownEmail.get("message")).isEqualTo(wrongPassword.get("message"));
    }

    // ------------------------------------------------------------------
    // Refresh-token reuse detection
    // ------------------------------------------------------------------

    @Test
    void refreshToken_reusedAfterRotation_returns401AndRevokesEveryUserSession() throws Exception {
        String registerBody = register("reuse@example.com", "reuse-password");
        String refreshToken1 = JsonPath.read(registerBody, "$.refreshToken");

        String rotatedBody = mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson(refreshToken1)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String refreshToken2 = JsonPath.read(rotatedBody, "$.refreshToken");

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson(refreshToken1)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Refresh token reuse detected"));

        // The token that reuse-detection rotated in is now dead too: proof that
        // revokeAllForUser ran and every session was killed, not just token #1.
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson(refreshToken2)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Refresh token reuse detected"));

        List<RefreshToken> stillActive = refreshTokenRepository.findAll().stream()
                .filter(token -> !token.isRevoked())
                .toList();
        assertThat(stillActive).isEmpty();
    }

    // ------------------------------------------------------------------
    // Protected endpoint — RestAuthenticationEntryPoint under real HTTP
    // ------------------------------------------------------------------

    @Test
    void getMe_withoutAuthorizationHeader_returns401WithAuthenticationRequiredShape() throws Exception {
        mockMvc.perform(get("/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Authentication required"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void getMe_withMalformedToken_returns401WithSameShapeAsNoHeader() throws Exception {
        String noHeaderBody = mockMvc.perform(get("/me"))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        String garbageTokenBody = mockMvc.perform(get("/me")
                        .header("Authorization", "Bearer not-a-real-jwt.abc.def"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Authentication required"))
                .andReturn().getResponse().getContentAsString();

        Map<String, Object> noHeader = JsonPath.read(noHeaderBody, "$");
        Map<String, Object> garbageToken = JsonPath.read(garbageTokenBody, "$");
        assertThat(garbageToken.keySet()).isEqualTo(noHeader.keySet());
        assertThat(garbageToken.get("message")).isEqualTo(noHeader.get("message"));
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    /** Registers a user through the real endpoint, asserts 201, returns the response body. */
    private String register(String email, String password) throws Exception {
        return mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(authJson(email, password)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private static String authJson(String email, String password) {
        return """
                {"email": "%s", "password": "%s"}
                """.formatted(email, password);
    }

    private static String refreshJson(String refreshToken) {
        return """
                {"refreshToken": "%s"}
                """.formatted(refreshToken);
    }
}

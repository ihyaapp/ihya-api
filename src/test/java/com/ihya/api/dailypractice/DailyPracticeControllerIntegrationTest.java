package com.ihya.api.dailypractice;

import com.ihya.api.catalogue.SunnahRepository;
import com.ihya.api.identity.RefreshTokenRepository;
import com.ihya.api.identity.UserRepository;
import com.ihya.api.notification.NotificationPreferencesRepository;
import com.ihya.api.notification.NotificationRepository;
import com.ihya.api.notification.PushTokenRepository;
import com.ihya.api.profile.ProfileRepository;
import com.ihya.api.profile.UserInterestRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack verification of the Phase 5 core habit loop
 * (docs/api-contract.md §2 "Daily practice"): {@code GET /assignment/today},
 * {@code POST /assignment/replacement}, {@code POST}/{@code PATCH}/{@code GET
 * /practices}, and {@code GET /me/progress}. Same conventions as
 * {@code CatalogueControllerIntegrationTest}/{@code NotificationControllerIntegrationTest}:
 * {@code ci} profile, real Postgres, nothing mocked. Flyway's {@code V10} seed
 * migration has already populated the catalogue by the time this runs, so
 * assignment/practice writes reference that real seeded content rather than
 * fixtures this test creates itself.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("ci")
class DailyPracticeControllerIntegrationTest {

    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("UTC"));

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private ProfileRepository profileRepository;
    @Autowired
    private UserInterestRepository userInterestRepository;
    @Autowired
    private NotificationPreferencesRepository notificationPreferencesRepository;
    @Autowired
    private PushTokenRepository pushTokenRepository;
    @Autowired
    private PracticeRepository practiceRepository;
    @Autowired
    private DailyAssignmentRepository dailyAssignmentRepository;
    @Autowired
    private UserProgressRepository userProgressRepository;
    @Autowired
    private SunnahRepository sunnahRepository;
    @Autowired
    private NotificationRepository notificationRepository;

    @BeforeEach
    @AfterEach
    void resetDatabase() {
        refreshTokenRepository.deleteAllInBatch();
        userInterestRepository.deleteAllInBatch();
        profileRepository.deleteAllInBatch();
        pushTokenRepository.deleteAllInBatch();
        notificationPreferencesRepository.deleteAllInBatch();
        notificationRepository.deleteAllInBatch();
        practiceRepository.deleteAllInBatch();
        dailyAssignmentRepository.deleteAllInBatch();
        userProgressRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    // ------------------------------------------------------------------
    // GET /assignment/today
    // ------------------------------------------------------------------

    @Test
    void getToday_firstCall_createsAssignmentFromSeededCatalogue() throws Exception {
        String accessToken = registerAndGetAccessToken();

        mockMvc.perform(get("/v1/assignment/today").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sunnah.slug").isNotEmpty())
                .andExpect(jsonPath("$.replacementAvailable").value(true));
    }

    @Test
    void getToday_secondCallSameDay_returnsSameAssignment() throws Exception {
        String accessToken = registerAndGetAccessToken();
        String firstBody = mockMvc.perform(get("/v1/assignment/today").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String firstSlug = JsonPath.read(firstBody, "$.sunnah.slug");

        mockMvc.perform(get("/v1/assignment/today").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sunnah.slug").value(firstSlug));
    }

    @Test
    void getToday_noToken_returns401() throws Exception {
        mockMvc.perform(get("/v1/assignment/today"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    // ------------------------------------------------------------------
    // POST /assignment/replacement
    // ------------------------------------------------------------------

    @Test
    void postReplacement_available_returnsSunnahAndMarksReplacementUsed() throws Exception {
        String accessToken = registerAndGetAccessToken();
        mockMvc.perform(get("/v1/assignment/today").header("Authorization", "Bearer " + accessToken));

        mockMvc.perform(post("/v1/assignment/replacement")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason": "not feeling it today"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sunnah.slug").isNotEmpty());
    }

    @Test
    void postReplacement_calledTwiceSameDay_secondReturns409() throws Exception {
        String accessToken = registerAndGetAccessToken();
        mockMvc.perform(get("/v1/assignment/today").header("Authorization", "Bearer " + accessToken));
        mockMvc.perform(post("/v1/assignment/replacement")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason": "first replacement"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/v1/assignment/replacement")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason": "second replacement"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void postReplacement_noToken_returns401() throws Exception {
        mockMvc.perform(post("/v1/assignment/replacement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason": "why not"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // POST /practices
    // ------------------------------------------------------------------

    @Test
    void postPractices_newPractice_returns201WithStreakOne() throws Exception {
        String accessToken = registerAndGetAccessToken();
        UUID sunnahId = seededSunnahId();

        mockMvc.perform(post("/v1/practices")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recordPracticeJson(sunnahId, "grateful")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.practice.sunnahId").value(sunnahId.toString()))
                .andExpect(jsonPath("$.practice.feeling").value("grateful"))
                .andExpect(jsonPath("$.streak").value(1))
                .andExpect(jsonPath("$.longestStreak").value(1))
                .andExpect(jsonPath("$.totalPracticed").value(1))
                .andExpect(jsonPath("$.milestoneUnlocked").doesNotExist());
    }

    @Test
    void postPractices_sameDayTwice_secondReturns409WithExistingState() throws Exception {
        String accessToken = registerAndGetAccessToken();
        UUID sunnahId = seededSunnahId();
        mockMvc.perform(post("/v1/practices")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recordPracticeJson(sunnahId, null)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/practices")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recordPracticeJson(sunnahId, "second attempt")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.streak").value(1));
    }

    @Test
    void postPractices_unknownSunnah_returns404() throws Exception {
        String accessToken = registerAndGetAccessToken();

        mockMvc.perform(post("/v1/practices")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recordPracticeJson(UUID.randomUUID(), null)))
                .andExpect(status().isNotFound());
    }

    @Test
    void postPractices_noToken_returns401() throws Exception {
        mockMvc.perform(post("/v1/practices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recordPracticeJson(UUID.randomUUID(), null)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postPractices_thirdConsecutiveDay_returns201WithMilestoneUnlocked() throws Exception {
        Registration registration = registerAndGetIds();
        UUID sunnahId = seededSunnahId();
        seedPriorPractice(registration.userId(), sunnahId, TODAY.minusDays(2));
        seedPriorPractice(registration.userId(), sunnahId, TODAY.minusDays(1));

        mockMvc.perform(post("/v1/practices")
                        .header("Authorization", "Bearer " + registration.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recordPracticeJson(sunnahId, null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.streak").value(3))
                .andExpect(jsonPath("$.milestoneUnlocked").value("streak_3"));
    }

    // ------------------------------------------------------------------
    // PATCH /practices/{id}
    // ------------------------------------------------------------------

    @Test
    void patchPractice_ownPractice_updatesFeeling() throws Exception {
        String accessToken = registerAndGetAccessToken();
        UUID sunnahId = seededSunnahId();
        String recordBody = mockMvc.perform(post("/v1/practices")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recordPracticeJson(sunnahId, null)))
                .andReturn().getResponse().getContentAsString();
        String practiceId = JsonPath.read(recordBody, "$.practice.id");

        mockMvc.perform(patch("/v1/practices/" + practiceId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"feeling": "at peace"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feeling").value("at peace"));
    }

    @Test
    void patchPractice_unknownId_returns404() throws Exception {
        String accessToken = registerAndGetAccessToken();

        mockMvc.perform(patch("/v1/practices/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"feeling": "at peace"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void patchPractice_noToken_returns401() throws Exception {
        mockMvc.perform(patch("/v1/practices/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"feeling": "at peace"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // GET /practices
    // ------------------------------------------------------------------

    @Test
    void getPractices_afterRecording_returnsHistoryWithEmbeddedSunnah() throws Exception {
        String accessToken = registerAndGetAccessToken();
        UUID sunnahId = seededSunnahId();
        mockMvc.perform(post("/v1/practices")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recordPracticeJson(sunnahId, null)));

        mockMvc.perform(get("/v1/practices").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].sunnahId").value(sunnahId.toString()))
                .andExpect(jsonPath("$.items[0].sunnah.id").value(sunnahId.toString()))
                .andExpect(jsonPath("$.nextCursor").doesNotExist());
    }

    @Test
    void getPractices_noToken_returns401() throws Exception {
        mockMvc.perform(get("/v1/practices"))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // GET /me/progress
    // ------------------------------------------------------------------

    @Test
    void getProgress_afterOnePractice_returnsStreakOne() throws Exception {
        String accessToken = registerAndGetAccessToken();
        UUID sunnahId = seededSunnahId();
        mockMvc.perform(post("/v1/practices")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recordPracticeJson(sunnahId, null)));

        mockMvc.perform(get("/v1/me/progress").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.streak").value(1))
                .andExpect(jsonPath("$.longestStreak").value(1))
                .andExpect(jsonPath("$.totalPracticed").value(1))
                .andExpect(jsonPath("$.earnedMilestoneKeys").isEmpty());
    }

    @Test
    void getProgress_freshlyRegisteredUser_returnsAllZeroes() throws Exception {
        String accessToken = registerAndGetAccessToken();

        mockMvc.perform(get("/v1/me/progress").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.streak").value(0))
                .andExpect(jsonPath("$.totalPracticed").value(0));
    }

    @Test
    void getProgress_noToken_returns401() throws Exception {
        mockMvc.perform(get("/v1/me/progress"))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private String registerAndGetAccessToken() throws Exception {
        return registerAndGetIds().accessToken();
    }

    private Registration registerAndGetIds() throws Exception {
        String email = "daily-practice-user-" + UUID.randomUUID() + "@example.com";
        String body = mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "correct-horse-battery"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(body, "$.accessToken");
        String userId = JsonPath.read(body, "$.userId");
        return new Registration(accessToken, UUID.fromString(userId));
    }

    /** Inserts a Practice + advances user_progress directly, bypassing the API — used to set up
     * a multi-day streak history that can't be produced through HTTP alone (practice_date is
     * always "today" server-side). */
    private void seedPriorPractice(UUID userId, UUID sunnahId, LocalDate practiceDate) {
        practiceRepository.save(new Practice(userId, sunnahId, practiceDate, null));
        UserProgress progress = userProgressRepository.findById(userId).orElseThrow();
        progress.applyPractice(practiceDate);
        userProgressRepository.save(progress);
    }

    private UUID seededSunnahId() {
        return sunnahRepository.findBySlug("use-the-miswak").orElseThrow().getId();
    }

    private static String recordPracticeJson(UUID sunnahId, String feeling) {
        return feeling == null
                ? """
                {"sunnahId": "%s"}
                """.formatted(sunnahId)
                : """
                {"sunnahId": "%s", "feeling": "%s"}
                """.formatted(sunnahId, feeling);
    }

    private record Registration(String accessToken, UUID userId) {
    }
}

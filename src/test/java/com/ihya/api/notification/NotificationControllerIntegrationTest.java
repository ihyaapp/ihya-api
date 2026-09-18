package com.ihya.api.notification;

import com.ihya.api.catalogue.SunnahRepository;
import com.ihya.api.dailypractice.DailyAssignmentRepository;
import com.ihya.api.dailypractice.Practice;
import com.ihya.api.dailypractice.PracticeRepository;
import com.ihya.api.dailypractice.UserProgress;
import com.ihya.api.dailypractice.UserProgressRepository;
import com.ihya.api.identity.RefreshTokenRepository;
import com.ihya.api.identity.UserRepository;
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack verification of the Phase 4 storage/API surface (docs/api-contract.md
 * §2 "Me & preferences"): {@code GET}/{@code PATCH /me/notification-preferences}
 * and {@code POST /me/push-tokens}. Same conventions as {@code AuthControllerIntegrationTest}
 * and {@code CatalogueControllerIntegrationTest}: {@code ci} profile, real
 * Postgres, nothing mocked.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("ci")
class NotificationControllerIntegrationTest {

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
    private NotificationRepository notificationRepository;
    @Autowired
    private SunnahRepository sunnahRepository;

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
    // GET /me/notification-preferences
    // ------------------------------------------------------------------

    @Test
    void getPreferences_freshlyRegisteredUser_returnsContractDefaults() throws Exception {
        String accessToken = registerAndGetAccessToken();

        mockMvc.perform(get("/v1/me/notification-preferences").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyReminder").value(true))
                .andExpect(jsonPath("$.streakReminder").value(true))
                .andExpect(jsonPath("$.weeklySummary").value(false))
                .andExpect(jsonPath("$.reminderTime").value("09:00"));
    }

    @Test
    void getPreferences_noToken_returns401() throws Exception {
        mockMvc.perform(get("/v1/me/notification-preferences"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    // ------------------------------------------------------------------
    // PATCH /me/notification-preferences
    // ------------------------------------------------------------------

    @Test
    void patchPreferences_partialBody_updatesOnlyGivenFieldsAndPersists() throws Exception {
        String accessToken = registerAndGetAccessToken();

        mockMvc.perform(patch("/v1/me/notification-preferences")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dailyReminder": false, "reminderTime": "18:30"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyReminder").value(false))
                .andExpect(jsonPath("$.streakReminder").value(true))
                .andExpect(jsonPath("$.weeklySummary").value(false))
                .andExpect(jsonPath("$.reminderTime").value("18:30"));

        mockMvc.perform(get("/v1/me/notification-preferences").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyReminder").value(false))
                .andExpect(jsonPath("$.reminderTime").value("18:30"));
    }

    @Test
    void patchPreferences_malformedReminderTime_returns400() throws Exception {
        String accessToken = registerAndGetAccessToken();

        mockMvc.perform(patch("/v1/me/notification-preferences")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reminderTime": "not-a-time"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // ------------------------------------------------------------------
    // POST /me/push-tokens
    // ------------------------------------------------------------------

    @Test
    void postPushToken_validBody_returns204() throws Exception {
        String accessToken = registerAndGetAccessToken();

        mockMvc.perform(post("/v1/me/push-tokens")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pushTokenJson("ExponentPushToken[abc]", "ios")))
                .andExpect(status().isNoContent());
    }

    @Test
    void postPushToken_sameTokenTwice_isIdempotentAndReturns204Both() throws Exception {
        String accessToken = registerAndGetAccessToken();
        String body = pushTokenJson("ExponentPushToken[repeat]", "android");

        mockMvc.perform(post("/v1/me/push-tokens")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/v1/me/push-tokens")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());
    }

    @Test
    void postPushToken_invalidPlatform_returns400() throws Exception {
        String accessToken = registerAndGetAccessToken();

        mockMvc.perform(post("/v1/me/push-tokens")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pushTokenJson("ExponentPushToken[abc]", "windows-phone")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postPushToken_noToken_returns401() throws Exception {
        mockMvc.perform(post("/v1/me/push-tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pushTokenJson("ExponentPushToken[abc]", "ios")))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // GET /notifications
    // ------------------------------------------------------------------

    @Test
    void getNotifications_freshlyRegisteredUser_returnsEmptyFeed() throws Exception {
        String accessToken = registerAndGetAccessToken();

        mockMvc.perform(get("/v1/notifications").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.nextCursor").doesNotExist());
    }

    @Test
    void getNotifications_afterMilestoneEarningPractice_includesUnreadMilestoneNotification() throws Exception {
        Registration registration = registerAndGetIds();
        UUID sunnahId = seededSunnahId();
        seedPriorPractice(registration.userId(), sunnahId, TODAY.minusDays(2));
        seedPriorPractice(registration.userId(), sunnahId, TODAY.minusDays(1));

        mockMvc.perform(post("/v1/practices")
                        .header("Authorization", "Bearer " + registration.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recordPracticeJson(sunnahId, null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.milestoneUnlocked").value("3-day-streak"));

        mockMvc.perform(get("/v1/notifications").header("Authorization", "Bearer " + registration.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].type").value("milestone_earned"))
                .andExpect(jsonPath("$.items[0].body", containsString("3 day streak")))
                .andExpect(jsonPath("$.items[0].read").value(false));
    }

    @Test
    void getNotifications_moreRowsThanLimit_paginatesWithCursor() throws Exception {
        Registration registration = registerAndGetIds();
        seedNotification(registration.userId(), "first");
        seedNotification(registration.userId(), "second");
        seedNotification(registration.userId(), "third");

        String firstPageBody = mockMvc.perform(get("/v1/notifications?limit=2")
                        .header("Authorization", "Bearer " + registration.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.nextCursor").exists())
                .andReturn().getResponse().getContentAsString();
        String nextCursor = JsonPath.read(firstPageBody, "$.nextCursor");

        mockMvc.perform(get("/v1/notifications?limit=2&cursor=" + nextCursor)
                        .header("Authorization", "Bearer " + registration.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.nextCursor").doesNotExist());
    }

    @Test
    void getNotifications_noToken_returns401() throws Exception {
        mockMvc.perform(get("/v1/notifications"))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // POST /notifications/read
    // ------------------------------------------------------------------

    @Test
    void postNotificationsRead_noBody_marksEveryUnreadNotificationAsRead() throws Exception {
        Registration registration = registerAndGetIds();
        Notification first = seedNotification(registration.userId(), "first");
        Notification second = seedNotification(registration.userId(), "second");

        mockMvc.perform(post("/v1/notifications/read")
                        .header("Authorization", "Bearer " + registration.accessToken()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/v1/notifications").header("Authorization", "Bearer " + registration.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(second.getId().toString()))
                .andExpect(jsonPath("$.items[0].read").value(true))
                .andExpect(jsonPath("$.items[1].id").value(first.getId().toString()))
                .andExpect(jsonPath("$.items[1].read").value(true));
    }

    @Test
    void postNotificationsRead_specificIds_marksOnlyThoseRead() throws Exception {
        Registration registration = registerAndGetIds();
        Notification first = seedNotification(registration.userId(), "first");
        Notification second = seedNotification(registration.userId(), "second");

        mockMvc.perform(post("/v1/notifications/read")
                        .header("Authorization", "Bearer " + registration.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ids": ["%s"]}
                                """.formatted(first.getId())))
                .andExpect(status().isNoContent());

        // Newest first: second was seeded after first, so it's items[0].
        mockMvc.perform(get("/v1/notifications").header("Authorization", "Bearer " + registration.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(second.getId().toString()))
                .andExpect(jsonPath("$.items[0].read").value(false))
                .andExpect(jsonPath("$.items[1].id").value(first.getId().toString()))
                .andExpect(jsonPath("$.items[1].read").value(true));
    }

    @Test
    void postNotificationsRead_noToken_returns401() throws Exception {
        mockMvc.perform(post("/v1/notifications/read"))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private String registerAndGetAccessToken() throws Exception {
        return registerAndGetIds().accessToken();
    }

    private Registration registerAndGetIds() throws Exception {
        String email = "notification-user-" + UUID.randomUUID() + "@example.com";
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

    private static String pushTokenJson(String expoPushToken, String platform) {
        return """
                {"expoPushToken": "%s", "platform": "%s"}
                """.formatted(expoPushToken, platform);
    }

    private Notification seedNotification(UUID userId, String label) {
        return notificationRepository.save(
                new Notification(userId, "milestone_earned", "Milestone unlocked!", "Seeded: " + label));
    }

    /** Same technique as {@code DailyPracticeControllerIntegrationTest}: inserts a Practice + advances
     * user_progress directly, bypassing the API, to set up a multi-day streak history that can't be
     * produced through HTTP alone (practice_date is always "today" server-side). */
    private void seedPriorPractice(UUID userId, UUID sunnahId, LocalDate practiceDate) {
        practiceRepository.save(new Practice(userId, sunnahId, practiceDate, null));
        UserProgress progress = userProgressRepository.findById(userId).orElseThrow();
        progress.applyPractice(practiceDate);
        userProgressRepository.save(progress);
    }

    private UUID seededSunnahId() {
        return sunnahRepository.findBySlug("use-the-miswak-before-prayer").orElseThrow().getId();
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

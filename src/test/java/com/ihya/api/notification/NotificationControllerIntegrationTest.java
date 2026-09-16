package com.ihya.api.notification;

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

import java.util.UUID;

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

    @BeforeEach
    @AfterEach
    void resetDatabase() {
        refreshTokenRepository.deleteAllInBatch();
        userInterestRepository.deleteAllInBatch();
        profileRepository.deleteAllInBatch();
        pushTokenRepository.deleteAllInBatch();
        notificationPreferencesRepository.deleteAllInBatch();
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
    // helpers
    // ------------------------------------------------------------------

    private String registerAndGetAccessToken() throws Exception {
        String email = "notification-user-" + UUID.randomUUID() + "@example.com";
        String body = mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "correct-horse-battery"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private static String pushTokenJson(String expoPushToken, String platform) {
        return """
                {"expoPushToken": "%s", "platform": "%s"}
                """.formatted(expoPushToken, platform);
    }
}

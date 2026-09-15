package com.ihya.api.catalogue;

import com.ihya.api.identity.Role;
import com.ihya.api.identity.User;
import com.ihya.api.identity.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack verification that the catalogue's role-based method security is
 * actually wired end to end — the one thing {@code CategoryServiceTest} /
 * {@code SunnahServiceTest} (both pure Mockito, no Spring context) cannot
 * catch, since they never exercise {@code @PreAuthorize}, {@code SecurityConfig},
 * or {@link com.ihya.api.identity.JwtAuthenticationFilter}'s authority lookup.
 *
 * <p>Same conventions as {@code AuthControllerIntegrationTest}: {@code ci}
 * profile, real Postgres, nothing mocked. Flyway's {@code V10} seed migration
 * has already populated the catalogue by the time this runs, so reads are
 * checked against that real content; every row this test itself creates uses
 * a {@code test-} prefixed slug and is deleted in {@link #cleanUp()} so the
 * seeded catalogue and other test runs are unaffected.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("ci")
class CatalogueControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private SunnahRepository sunnahRepository;

    @AfterEach
    void cleanUp() {
        sunnahRepository.findBySlug("test-sunnah").ifPresent(sunnahRepository::delete);
        categoryRepository.findBySlug("test-category").ifPresent(categoryRepository::delete);
    }

    // ------------------------------------------------------------------
    // Reads: open to any authenticated user
    // ------------------------------------------------------------------

    @Test
    void getCategories_authenticatedUser_returns200WithSeededContent() throws Exception {
        String accessToken = registerAndGetAccessToken();

        mockMvc.perform(get("/v1/categories").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.slug == 'faith-worship')]").exists())
                .andExpect(jsonPath("$[?(@.slug == 'faith-worship')].sunnahCount").value(3));
    }

    @Test
    void getSunnahs_authenticatedUser_returns200WithSeededContent() throws Exception {
        String accessToken = registerAndGetAccessToken();

        mockMvc.perform(get("/v1/sunnahs").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.slug == 'use-the-miswak')].categorySlug").value("health-cleanliness"));
    }

    @Test
    void getCategories_noToken_returns401() throws Exception {
        mockMvc.perform(get("/v1/categories"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    // ------------------------------------------------------------------
    // Writes: ADMIN only
    // ------------------------------------------------------------------

    @Test
    void postCategory_regularUser_returns403WithErrorShape() throws Exception {
        String accessToken = registerAndGetAccessToken();

        mockMvc.perform(post("/v1/categories")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryJson("test-category", "Test Category")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void fullAdminWriteFlow_createUpdateThenNonAdminSunnahWritesAreRejected() throws Exception {
        String adminToken = registerAdminAndGetAccessToken();
        String userToken = registerAndGetAccessToken();

        mockMvc.perform(post("/v1/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryJson("test-category", "Test Category")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("test-category"))
                .andExpect(jsonPath("$.status").value("active"))
                .andExpect(jsonPath("$.sunnahCount").value(0));

        mockMvc.perform(patch("/v1/categories/test-category")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description": "Updated by an admin"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated by an admin"));

        String sunnahBody = mockMvc.perform(post("/v1/sunnahs")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"slug": "test-sunnah", "title": "Test Sunnah", "categorySlug": "test-category",
                                 "source": "Test Source", "description": "Test description",
                                 "reflection": "Test reflection"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("test-sunnah"))
                .andReturn().getResponse().getContentAsString();
        String sunnahId = JsonPath.read(sunnahBody, "$.id");

        mockMvc.perform(get("/v1/sunnahs/test-sunnah").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sunnahId));

        // A non-admin cannot delete, even though they can read.
        mockMvc.perform(delete("/v1/sunnahs/test-sunnah").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/v1/sunnahs/test-sunnah").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/v1/sunnahs/test-sunnah").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private String registerAndGetAccessToken() throws Exception {
        String email = "catalogue-user-" + UUID.randomUUID() + "@example.com";
        String body = mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(authJson(email, "correct-horse-battery")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    /**
     * Registers a plain user through the real HTTP flow, then flips their role
     * to ADMIN directly in the database — {@code User} has no role setter and
     * no request path can grant ADMIN, by design (see {@link Role}'s javadoc:
     * "the first admin is provisioned by a direct database update"). Reusing
     * that same reflective field-set the existing catalogue unit tests already
     * use for read-only entity fields is the only way to simulate it here.
     */
    private String registerAdminAndGetAccessToken() throws Exception {
        String email = "catalogue-admin-" + UUID.randomUUID() + "@example.com";
        String body = mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(authJson(email, "correct-horse-battery")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String userId = JsonPath.read(body, "$.userId");

        User user = userRepository.findById(UUID.fromString(userId)).orElseThrow();
        try {
            Field roleField = User.class.getDeclaredField("role");
            roleField.setAccessible(true);
            roleField.set(user, Role.ADMIN);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not set User.role for test fixture", e);
        }
        userRepository.saveAndFlush(user);

        return JsonPath.read(body, "$.accessToken");
    }

    private static String authJson(String email, String password) {
        return """
                {"email": "%s", "password": "%s"}
                """.formatted(email, password);
    }

    private static String categoryJson(String slug, String name) {
        return """
                {"slug": "%s", "name": "%s"}
                """.formatted(slug, name);
    }
}

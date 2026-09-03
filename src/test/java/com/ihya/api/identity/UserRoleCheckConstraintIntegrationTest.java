package com.ihya.api.identity;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Proves the {@code chk_users_role} CHECK constraint (migration V4) actually
 * exists in the schema and is enforced by Postgres.
 *
 * <p>Deliberately bypasses the JPA layer with raw SQL: {@code User.role} is a
 * {@code @Enumerated(EnumType.STRING)} field, so the entity can never carry an
 * invalid value in the first place. The constraint's whole job is to guard the
 * one path that skips the app — a manual/direct SQL write — so the test has to
 * take that path too. Runs against the real {@code ci} Postgres, same as the
 * other integration tests.
 */
@SpringBootTest
@ActiveProfiles("ci")
class UserRoleCheckConstraintIntegrationTest {

    private static final String TEST_EMAIL = "role-constraint-probe@example.com";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM users WHERE email = ?", TEST_EMAIL);
    }

    @Test
    void directInsertWithInvalidRole_isRejectedByDatabase() {
        String insertWithBogusRole = "INSERT INTO users (email, password_hash, role) VALUES (?, ?, ?)";

        Throwable thrown = catchThrowable(() ->
                jdbcTemplate.update(insertWithBogusRole, TEST_EMAIL, "irrelevant-hash", "BOGUS"));

        assertThat(thrown)
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_users_role");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM users WHERE email = ?", Integer.class, TEST_EMAIL))
                .isZero();
    }
}

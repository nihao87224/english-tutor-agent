package cn.forever24.tutor.infrastructure.auth;

import cn.forever24.tutor.application.auth.AuthenticatedUser;
import cn.forever24.tutor.application.auth.UserAccountRepository;
import cn.forever24.tutor.application.auth.UserCredentials;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

public class JdbcUserAccountRepository implements UserAccountRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcUserAccountRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean existsByNormalizedEmail(String normalizedEmail) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM app_user WHERE email_normalized = ?",
                Integer.class,
                normalizedEmail);
        return count != null && count > 0;
    }

    @Override
    public Optional<UserCredentials> findCredentialsByNormalizedEmail(String normalizedEmail) {
        try {
            CredentialRow row = jdbcTemplate.queryForObject(
                    """
                            SELECT id, password_hash
                            FROM app_user
                            WHERE email_normalized = ?
                            """,
                    (rs, rowNum) -> new CredentialRow(rs.getLong("id"), rs.getString("password_hash")),
                    normalizedEmail);
            if (row == null) {
                return Optional.empty();
            }
            return findById(row.userId()).map(user -> new UserCredentials(user, row.passwordHash()));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<AuthenticatedUser> findByNormalizedEmail(String normalizedEmail) {
        try {
            Long userId = jdbcTemplate.queryForObject(
                    "SELECT id FROM app_user WHERE email_normalized = ?",
                    Long.class,
                    normalizedEmail);
            return userId == null ? Optional.empty() : findById(userId);
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<AuthenticatedUser> findById(long userId) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    """
                            SELECT id, user_key, email, status, locale, timezone, auth_version
                            FROM app_user
                            WHERE id = ?
                            """,
                    (rs, rowNum) -> new AuthenticatedUser(
                            rs.getLong("id"),
                            rs.getString("user_key"),
                            rs.getString("email"),
                            rs.getString("status"),
                            rs.getString("locale"),
                            rs.getString("timezone"),
                            rs.getLong("auth_version"),
                            rolesForUser(userId),
                            authoritiesForUser(userId)),
                    userId));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    @Override
    public AuthenticatedUser createUser(
            String userKey,
            String email,
            String normalizedEmail,
            String passwordHash,
            String locale,
            String timezone,
            Instant createdAt
    ) {
        jdbcTemplate.update("""
                        INSERT INTO app_user
                            (user_key, email, email_normalized, password_hash, status, timezone, locale,
                             auth_version, created_at_utc, updated_at_utc, version)
                        VALUES (?, ?, ?, ?, 'ACTIVE', ?, ?, 0, ?, ?, 0)
                        """,
                userKey,
                email,
                normalizedEmail,
                passwordHash,
                timezone,
                locale,
                Timestamp.from(createdAt),
                Timestamp.from(createdAt));
        return findByNormalizedEmail(normalizedEmail).orElseThrow();
    }

    @Override
    public void assignRole(long userId, String roleCode) {
        jdbcTemplate.update("""
                        INSERT IGNORE INTO app_user_role (user_id, role_id, created_at_utc)
                        SELECT ?, id, CURRENT_TIMESTAMP(3)
                        FROM app_role
                        WHERE code = ?
                        """,
                userId,
                roleCode);
    }

    @Override
    public boolean hasRole(String roleCode) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM app_user_role ur
                        JOIN app_role r ON r.id = ur.role_id
                        WHERE r.code = ?
                        """,
                Integer.class,
                roleCode);
        return count != null && count > 0;
    }

    @Override
    public void updateLastLogin(long userId, Instant lastLoginAt) {
        jdbcTemplate.update("""
                        UPDATE app_user
                        SET last_login_at_utc = ?,
                            updated_at_utc = ?,
                            version = version + 1
                        WHERE id = ?
                        """,
                Timestamp.from(lastLoginAt),
                Timestamp.from(lastLoginAt),
                userId);
    }

    private Set<String> rolesForUser(long userId) {
        return Set.copyOf(jdbcTemplate.queryForList("""
                        SELECT r.code
                        FROM app_user_role ur
                        JOIN app_role r ON r.id = ur.role_id
                        WHERE ur.user_id = ?
                        ORDER BY r.code
                        """,
                String.class,
                userId));
    }

    private Set<String> authoritiesForUser(long userId) {
        return Set.copyOf(jdbcTemplate.queryForList("""
                        SELECT p.code
                        FROM app_user_role ur
                        JOIN app_role_permission rp ON rp.role_id = ur.role_id
                        JOIN app_permission p ON p.id = rp.permission_id
                        WHERE ur.user_id = ?
                        ORDER BY p.code
                        """,
                String.class,
                userId));
    }

    private record CredentialRow(long userId, String passwordHash) {
    }
}

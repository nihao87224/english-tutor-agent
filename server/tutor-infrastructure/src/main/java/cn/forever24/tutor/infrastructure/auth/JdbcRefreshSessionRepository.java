package cn.forever24.tutor.infrastructure.auth;

import cn.forever24.tutor.application.auth.RefreshSessionDraft;
import cn.forever24.tutor.application.auth.RefreshSessionRepository;
import cn.forever24.tutor.application.auth.StoredRefreshSession;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

public class JdbcRefreshSessionRepository implements RefreshSessionRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcRefreshSessionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void create(RefreshSessionDraft session) {
        jdbcTemplate.update("""
                        INSERT INTO auth_refresh_session
                            (id, user_id, token_hash, client_type, device_name, auth_version,
                             expires_at_utc, created_at_utc, last_used_at_utc)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                session.id(),
                session.userId(),
                session.tokenHash(),
                session.clientType(),
                session.deviceName(),
                session.authVersion(),
                Timestamp.from(session.expiresAt()),
                Timestamp.from(session.createdAt()),
                Timestamp.from(session.createdAt()));
    }

    @Override
    public Optional<StoredRefreshSession> findByTokenHash(String tokenHash) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    """
                            SELECT id, user_id, token_hash, auth_version, expires_at_utc, revoked_at_utc
                            FROM auth_refresh_session
                            WHERE token_hash = ?
                            """,
                    (rs, rowNum) -> new StoredRefreshSession(
                            rs.getString("id"),
                            rs.getLong("user_id"),
                            rs.getString("token_hash"),
                            rs.getLong("auth_version"),
                            rs.getTimestamp("expires_at_utc").toInstant(),
                            rs.getTimestamp("revoked_at_utc") == null ? null : rs.getTimestamp("revoked_at_utc").toInstant()),
                    tokenHash));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    @Override
    public void revoke(String sessionId, Instant revokedAt) {
        jdbcTemplate.update("""
                        UPDATE auth_refresh_session
                        SET revoked_at_utc = COALESCE(revoked_at_utc, ?)
                        WHERE id = ?
                        """,
                Timestamp.from(revokedAt),
                sessionId);
    }

    @Override
    public void revokeAndReplace(String sessionId, String replacementSessionId, Instant revokedAt) {
        jdbcTemplate.update("""
                        UPDATE auth_refresh_session
                        SET revoked_at_utc = COALESCE(revoked_at_utc, ?),
                            replaced_by_id = ?
                        WHERE id = ?
                        """,
                Timestamp.from(revokedAt),
                replacementSessionId,
                sessionId);
    }
}

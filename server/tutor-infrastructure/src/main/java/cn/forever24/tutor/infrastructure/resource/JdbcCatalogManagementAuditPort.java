package cn.forever24.tutor.infrastructure.resource;

import cn.forever24.tutor.application.resource.CatalogManagementAuditPort;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;

public final class JdbcCatalogManagementAuditPort implements CatalogManagementAuditPort {
    private final JdbcTemplate jdbcTemplate;

    public JdbcCatalogManagementAuditPort(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate);
    }

    @Override
    public void append(long actorUserId, String action, String targetKey, Instant occurredAt) {
        jdbcTemplate.update("""
                        INSERT INTO admin_audit_log (actor_user_id, action_code, target_type, target_key, created_at_utc)
                        VALUES (?, ?, 'LEARNING_RESOURCE', ?, ?)
                        """, actorUserId, action, targetKey, Timestamp.from(occurredAt));
    }
}

package cn.forever24.tutor.infrastructure.entitlement;

import cn.forever24.tutor.application.entitlement.EntitlementAuditPort;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;

public final class JdbcEntitlementAuditPort implements EntitlementAuditPort {

    private final JdbcTemplate jdbcTemplate;

    public JdbcEntitlementAuditPort(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void append(
            long actorUserId,
            String actionCode,
            String entitlementKey,
            String beforeState,
            String afterState,
            Instant occurredAt
    ) {
        jdbcTemplate.update("""
                        INSERT INTO admin_audit_log (
                            actor_user_id, action_code, target_type, target_key,
                            before_json, after_json, created_at_utc
                        ) VALUES (?, ?, 'ENTITLEMENT', ?, ?, ?, ?)
                        """,
                actorUserId,
                actionCode,
                entitlementKey,
                jsonState(beforeState),
                jsonState(afterState),
                Timestamp.from(occurredAt));
    }

    private static String jsonState(String state) {
        return state == null ? null : "{\"state\":\"" + state + "\"}";
    }
}

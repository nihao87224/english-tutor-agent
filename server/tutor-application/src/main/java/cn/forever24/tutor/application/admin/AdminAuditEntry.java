package cn.forever24.tutor.application.admin;

import java.time.Instant;

public record AdminAuditEntry(
        long id,
        Long actorUserId,
        String actorEmail,
        String actionCode,
        String targetType,
        String targetKey,
        Instant createdAt
) {
}

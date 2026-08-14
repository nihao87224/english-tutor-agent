package cn.forever24.tutor.application.auth;

import java.time.Instant;

public record StoredRefreshSession(
        String id,
        long userId,
        String tokenHash,
        long authVersion,
        Instant expiresAt,
        Instant revokedAt
) {
}
